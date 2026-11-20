# exemplo-testes: processo de pagamento

Modelo de **automação de testes e barreiras de build** para serviços Java 25 + Quarkus 3.33. O domínio é pequeno de propósito: o processo de pagamento do [`exemplo-negocial`](../exemplo-negocial/), sem as entidades acessórias. Ficam a máquina de estados e os fluxos que mudam com o tipo do processo. Tudo o que importa está no build: `./mvnw verify` prova o comportamento e a estrutura, seja quem for (pessoa, ou agente de IA mais simples ou mais capaz) que mexeu no código.

Requisitos: JDK 25 (`sdk env install`) e Docker.

```bash
./mvnw verify                     # tudo: unidade, componente, arquitetura, aceitação e os *IT contra o jar
./mvnw verify -Pauditoria         # + teste de mutação (PITest) no domínio
```

## O domínio em uma tela

Um processo é aberto (tipo, cota, valor) e anda por ações. Quem diz se a ação vale é a **tabela do fluxo do tipo** (State dirigido por tabela):

| Regra | O quê |
|---|---|
| R1 | Abertura: cota obrigatória (até 30 caracteres) e valor maior que zero, com até 2 casas |
| R2 | Só as transições da tabela do tipo valem; o resto é `409` |
| R3 | Imobiliário passa pela vistoria depois da análise; serviço não tem vistoria |
| R4 | Alçada (`processo.alcada`, 50.000,00): acima, aguarda a alçada; igual ou abaixo, aprova direto |
| R5 | Solicitar documento, devolver para análise, reprovar e cancelar exigem motivo |
| R6 | Pago, reprovado e cancelado são finais: nenhuma ação |
| R7 | Cancelar vale em todo estado não final que o tipo usa |
| R8 | Toda transição vai ao histórico, com quem fez (cabeçalho `X-Usuario`) e o motivo |

API: `POST /processos`, `GET /processos/{id}`, `GET /processos/{id}/acoes`, `POST /processos/{id}/acoes/{ACAO}` (corpo opcional `{"motivo": "..."}`), `GET /processos/{id}/historico`. Documentação em `/q/openapi`.

## Os níveis de teste

Cada comportamento é provado no nível mais baixo que consegue prová-lo. Os de cima provam que a regra está **ligada**, não que está **certa**.

| Nível | Onde | Como | O que prova |
|---|---|---|---|
| Unidade: domínio | `domain/**` | JUnit puro e jqwik | A regra: a **tabela de cada fluxo escrita por extenso** (`TabelaDosFluxosTest`), propriedades em caminhos gerados (`FluxosPropriedadesTest`) e as recusas que não mudam nada |
| Unidade: use case | `application/**` | `@QuarkusTest` + `@InjectMock` nas portas | A orquestração com a configuração real: alçada chega ao domínio, o que é gravado, o que não é |
| Componente | `componente/*Test` | `@QuarkusTest` + RestAssured + PostgreSQL (Dev Services) | Persistência do estado e do histórico, `400` com os campos, `404`, `409`, formato único de erro |
| Integração | `componente/*IT`, `cucumber/*IT` | `@QuarkusIntegrationTest` herdando o `*Test` | O **jar empacotado** (perfil prod) se comporta igual |
| Aceitação | `src/test/resources/cucumber/features/*.feature` + `src/test/java/cucumber/` | Cucumber com Gherkin **em português** | As regras de negócio de ponta a ponta, escritas como o negócio fala: o analista lê e valida a especificação |
| Arquitetura | `arquitetura/` | ArchUnit (58 regras com ID e "Corrija:") | Camadas, nomes, domínio puro, nenhum ciclo |
| Contrato | `ContratoRestTest` + openapi-diff | contra `docs/api/openapi-base.json` | Nenhum campo de resposta some ou muda de tipo |
| Mutação | `-Pauditoria` | PITest no domínio, limite de 94% | Que os testes afirmam a regra, não só passam por ela |

## Aceitação: massa e cenários em tabelas, validados pelo negócio

O modelo de planilhas (uma de massa, outras de cenários), só que em **texto versionado no git**: o QA preenche tabelas, o negócio as lê e valida no PR, e o build as executa. Tudo fica em `cucumber/`, separado dos outros testes.

**A massa** (`src/test/resources/cucumber/massa/processos.md`) é uma tabela Markdown com os processos que já existem quando os cenários começam. A chave é natural (a cota) e os termos são os do negócio:

```
| cota     | tipo        | valor     | situação            | observação                       |
|----------|-------------|-----------|---------------------|----------------------------------|
| COTA-005 | serviço     | 50.000,00 | Em análise          | exatamente na alçada (4.1)       |
| COTA-006 | serviço     | 50.000,01 | Em análise          | um centavo acima da alçada (4.2) |
```

**Os cenários** (`src/test/resources/cucumber/features/`) são um arquivo por família, como as abas de uma planilha. Cada linha de `Exemplos` é um cenário:

```gherkin
# language: pt
Funcionalidade: Ações sobre o processo (R2 a R7)

  Esquema do Cenário: <cenário> <descrição>
    Dado o processo da cota "<cota>" da massa
    Quando "<quem>" pedir para "<ação>" com o motivo "<motivo>"
    Então a ação é "<resultado>" e o processo fica "<situação>"
    E a mensagem é "<mensagem>"
    E as ações disponíveis são "<ações disponíveis>"

    Exemplos:
      | cenário | regra | descrição                         | cota     | quem | ação               | motivo | resultado | situação          | mensagem | ações disponíveis |
      | 4.1     | R4    | Valor igual à alçada aprova       | COTA-005 | ana  | concluir a análise |        | aceita    | Aprovado          |          |                   |
      | 4.2     | R4    | Um centavo acima aguarda a alçada | COTA-006 | ana  | concluir a análise |        | aceita    | Aguardando alçada |          |                   |
```

| Aba | O que tem |
|---|---|
| `01-abertura.feature` | abrir processo: aceita ou recusada, o número e a situação |
| `02-acoes.feature` | uma ação sobre um processo da massa: resultado, situação, mensagem, ações disponíveis |
| `03-fluxos.feature` | vários passos seguidos (tabela de passos) e o histórico |

- **Célula vazia = não conferir.** A exceção é `motivo`: vazio manda a ação sem motivo.
- **Validação antes de rodar:** a massa é validada inteira antes de a aplicação subir. Termo, valor ou cota inválidos param com `processos.md, linha N` e a lista dos termos válidos.
- **Barreiras nos cenários:** cota que não está na massa, ou usada por dois cenários, falha dizendo qual.
- **Esperado divergente:** a falha fala a língua da tabela, por exemplo `expected: <Aprovado> but was: <Aguardando alçada>`.
- **Vocabulário único** (`src/test/java/cucumber/passos/Vocabulario.java`): situações, ações, tipos e valores como o negócio os diz, ligados ao código da API.
- **Relatórios:** `target/cucumber/ProcessoAceitacaoTest.html` (e `...IT.html`), com cada cenário e passo. Nos relatórios do surefire e do failsafe, cada teste tem o nome do cenário (`02-acoes.feature | 4.2 Valor um centavo acima...`).
- **Como preencher:** [skill teste-aceitacao, como preencher](.github/skills/teste-aceitacao/references/escrita.md).

O Cucumber está no lugar do Karate porque as palavras-chave do Karate são fixas em inglês (`# language: pt` não é aceito pelo parser dele) e os passos são a DSL dele, não frases do negócio.

Por que texto versionado e não planilhas de massa e cenários (como num motor Citrus), e como levar planilhas existentes para cá: [ADR 001](docs/adr/ADR_001_aceitacao_em_texto_versionado.md).

## IDs reprodutíveis na aceitação

Toda execução da aceitação (`*Test` e `*IT`) sobe **um PostgreSQL novo** e o prepara antes de a aplicação subir (`src/test/java/cucumber/BancoDaAceitacao.java`):

1. aplica as migrations de produção (`db/migration`);
2. grava a massa de `cucumber/massa/processos.md` (`MassaDeProcessos`): a sentinela com o id 1 e as linhas do arquivo com 2, 3... na ordem do arquivo. Depois faz as sequências recomeçarem em **1000**.

Por isso, em todo build e nas duas fases:

- o cenário parte direto da situação que precisa, citando a **cota** (a chave natural), nunca um número. Linha nova em qualquer posição da massa não muda nenhum cenário;
- o que os cenários abrem ganha 1000, 1001... na ordem dos arquivos (o prefixo numérico) e dos cenários, e o cenário pode conferir o número (`número` = 1000 na aba de abertura).

Para manter:

- **Situação nova:** uma linha na massa, com uma cota nova.
- **Uma cota por cenário** (cada linha de `Exemplos` conta como um). O passo `Dado o processo da cota` barra a cota usada por outro cenário e confere que o processo está como a massa diz.
- **Número gerado só se confere onde ele é o ponto do cenário:** um cenário novo que cria registro antes de outro muda o número dos seguintes.
- O recurso devolve **só o endereço do banco**.

### Por que não conflita com os outros testes

A aceitação roda num sandbox, e o isolamento está garantido por construção, não pela ordem dos testes:

| Garantia | Como | O que impede |
|---|---|---|
| Banco próprio | `BancoDaAceitacao` sobe um PostgreSQL novo; o `@WithTestResource` vale só para a classe de aceitação | Os testes de componente (banco do Dev Services) consumirem IDs ou verem a massa |
| JVM própria | Execuções separadas no `pom.xml`: `cucumber` no surefire e `cucumber-it` no failsafe, depois das demais | O estado do Quarkus passar de um teste para outro. No `*IT`, a configuração devolvida por um recurso de teste vale para todos os jars que sobem depois na mesma JVM |
| Sentinela | O carregador grava `COTA-SENTINELA` (id 1, que o arquivo do QA não pode usar) e o runner a confere antes do primeiro cenário | Rodar os cenários sobre o banco errado. A falha diz "a aplicação não está sobre o banco da aceitação" |
| Em sequência | Cucumber com `--order lexical`, um cenário de cada vez, e o `AceitacaoSequencialTest`, que falha se a execução paralela do JUnit for ligada | IDs gerados que mudam a cada build |

Verificado: o build passa também com a ordem invertida (`-Dsurefire.runOrder=reversealphabetical -Dfailsafe.runOrder=reversealphabetical`), com os mesmos IDs. Sem o `@WithTestResource`, a sentinela barra antes do primeiro cenário.

Detalhes e armadilhas: [skill teste-aceitacao, IDs fixos](.github/skills/teste-aceitacao/references/ids-fixos.md).

## Barreiras do build (da mais barata para a mais cara)

| Fase | Barreira | Pega |
|---|---|---|
| validate | Enforcer | JDK/Maven errados, Lombok, MapStruct, Panache, versões divergentes |
| compile | ErrorProne + NullAway (`-Werror`) | Nulo não declarado, erros semânticos, aviso de qualquer tipo |
| compile | Hibernate Processor | HQL inválido nas `@Query` |
| test | ArchUnit, unidade, componente, aceitação | Estrutura e comportamento |
| verify | `*IT` contra o jar, openapi-diff | Empacotamento e quebra de contrato |
| `-Pauditoria` | PITest | Teste que não afirma a regra |

Para o agente de IA: `AGENTS.md` (sempre ativo), as skills em `.github/skills/` (com o link em `.claude/skills/`) e os hooks em `.github/hooks/`. Os hooks negam a edição de `arquitetura/`, das migrations aplicadas e dos próprios hooks, e só deixam o turno terminar com a arquitetura e o domínio verdes (cerca de 10 s, sem Docker). O `CODEOWNERS` exige revisão do dono nessas partes e na tabela dos fluxos.

### Prova de que as barreiras pegam

Quebras feitas de propósito na criação deste modelo, e o que o build disse:

- Sem o `@WithTestResource(BancoDaAceitacao.class)`, a aceitação para antes do primeiro cenário com "A aplicação não está sobre o banco da aceitação" (sentinela), antes de qualquer cenário.
- `@ApplicationScoped` em `ProcessoPagamento` falha com `DOM-001: DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK`, junto com o "Corrija:".
- Um imobiliário aprovado direto da análise (`CONCLUIR_ANALISE -> APROVADO`) é pego duas vezes:
  - `TabelaDosFluxosTest` falha com "Tabela do fluxo IMOBILIARIO diferente da especificada" e mostra a diferença;
  - `FluxosPropriedadesTest.vistoriaSoNoImobiliarioESempreAntesDeAprovar` também falha.
- Na massa, um termo fora do vocabulário ("Aguardando alcada", sem cedilha) para o build antes de a aplicação subir, com a linha e a lista dos termos válidos.
- Nos cenários, cota fora da massa falha dizendo a cota, e a mesma cota em duas linhas de `Exemplos` falha dizendo o cenário dono. Esperado divergente falha na língua da tabela (`expected: <Aprovado> but was: <Aguardando alçada>`).
- Uma linha nova no meio da massa (que desloca os ids de todas as outras) não muda nenhum resultado.
