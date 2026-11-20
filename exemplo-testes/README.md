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
| Integração | `componente/*IT`, `aceitacao/*IT` | `@QuarkusIntegrationTest` herdando o `*Test` | O **jar empacotado** (perfil prod) se comporta igual |
| Aceitação | `aceitacao/` + `src/test/resources/aceitacao/processo/` | Karate lendo `cenarios.yaml` | Cenários de negócio de ponta a ponta, que o analista revisa no diff |
| Arquitetura | `arquitetura/` | ArchUnit (58 regras com ID e "Corrija:") | Camadas, nomes, domínio puro, nenhum ciclo |
| Contrato | `ContratoRestTest` + openapi-diff | contra `docs/api/openapi-base.json` | Nenhum campo de resposta some ou muda de tipo |
| Mutação | `-Pauditoria` | PITest no domínio, limite de 94% | Que os testes afirmam a regra, não só passam por ela |

## IDs reprodutíveis no Karate

Toda execução da aceitação (`*Test` e `*IT`) sobe **um PostgreSQL novo** e o prepara antes de a aplicação subir (`aceitacao/BancoDaAceitacao.java`):

1. aplica as migrations de produção (`db/migration`);
2. aplica a massa `src/test/resources/db/massa/R__massa_aceitacao.sql`, que grava um processo pronto em cada situação, **com ID fixo** (2 a 11; o 1 é a sentinela), e reinicia as sequências em **1000**.

Por isso, em todo build e nas duas fases:

- o cenário parte direto da situação que precisa: `processo: 3` é sempre o imobiliário aguardando vistoria;
- o que os cenários criam ganha 1000, 1001... na ordem de `cenarios.yaml`, e o cenário pode conferir o ID (`espera: { id: 1000 }`).

Configuração mínima: uma classe genérica, um SQL e `@WithTestResource(BancoDaAceitacao.class)` no runner. Para manter:

- **Situação nova:** uma linha no SQL com o próximo ID livre (abaixo de 1000).
- **Um registro da massa por cenário.** `Cenarios.validar` barra, antes de qualquer requisição, ID repetido entre cenários e ID que não é da massa.
- **ID gerado só se confere onde ele é o ponto do cenário:** um cenário novo que cria registro antes de outro muda o número dos seguintes.
- O recurso devolve **só o endereço do banco**.

### Por que não conflita com os outros testes

A aceitação roda num sandbox, e o isolamento está garantido por construção, não pela ordem dos testes:

| Garantia | Como | O que impede |
|---|---|---|
| Banco próprio | `BancoDaAceitacao` sobe um PostgreSQL novo; o `@WithTestResource` vale só para a classe de aceitação | Os testes de componente (banco do Dev Services) consumirem IDs ou verem a massa |
| JVM própria | Execuções separadas no `pom.xml`: `aceitacao` no surefire e `aceitacao-it` no failsafe, depois das demais | O estado do Quarkus passar de um teste para outro. No `*IT`, a configuração devolvida por um recurso de teste vale para todos os jars que sobem depois na mesma JVM |
| Sentinela | O `@setup` do Karate confere o processo 1 (`COTA-SENTINELA`, que nenhum cenário usa) antes de qualquer cenário | Rodar os cenários sobre o banco errado. A falha diz "a aplicação não está sobre o banco da aceitação" |
| Em sequência | `Karate.run` (não `Runner.parallel`) e o `AceitacaoSequencialTest`, que falha se a execução paralela do JUnit for ligada | IDs gerados que mudam a cada build |

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

- Sem o `@WithTestResource(BancoDaAceitacao.class)`, o Karate para no `@setup` com "A aplicação não está sobre o banco da aceitação" (sentinela), antes de qualquer cenário.
- `@ApplicationScoped` em `ProcessoPagamento` falha com `DOM-001: DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK`, junto com o "Corrija:".
- Um imobiliário aprovado direto da análise (`CONCLUIR_ANALISE -> APROVADO`) é pego duas vezes:
  - `TabelaDosFluxosTest` falha com "Tabela do fluxo IMOBILIARIO diferente da especificada" e mostra a diferença;
  - `FluxosPropriedadesTest.vistoriaSoNoImobiliarioESempreAntesDeAprovar` também falha.
- Um defeito de verdade nos passos do Karate (o motivo ia literal, `#(passo.motivo)`) foi pego pelo cenário 5.1, que confere o histórico.
