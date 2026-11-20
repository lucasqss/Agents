# ADR 001: aceitação em texto versionado, executada pelo Cucumber no build

- Situação: aceita
- Data: 2026-11-20

## Contexto

Queremos que os QAs mantenham **massa de dados** (os cadastros de que os testes precisam) e **cenários de teste** sem escrever código, que o negócio valide o que é testado e que tudo rode no `mvn clean install`.

Já usamos um modelo assim, com Citrus: os QAs preenchiam uma planilha de massa e outra de cenários, e um motor as executava. O problema recorrente eram **planilhas sobrescritas**: um arquivo binário editado por várias pessoas, sem merge, sem histórico útil e sem revisão.

As planilhas de exemplo da migração do Selic (massa de participantes, contas e títulos; cenários de comandos, de operação e de carteira) mostram ainda que:

1. **São planilhas de apresentação, não de dados.** Têm células mescladas e o participante só na primeira linha das suas contas. Trazem legendas nas colunas laterais e código junto da descrição ("01 - Normal - Livre"). Algumas células misturam campos ("210100 01/03/2026", "60746948 ou 00000000") e há zeros à esquerda perdidos (`6077` × `00006077`).
2. **Os cenários de carteira têm vários passos** (1º comando, 2º comando...), com saldo, recompra e revenda conferidos depois de cada um.
3. **O resultado esperado inclui as mensagens enviadas** (`1052R1 ==> Cedente`, `1611`, `STR ==> ATU`), ou seja, mensageria do SPB.
4. **O volume é de matriz:** a aba de carteira tem cerca de mil linhas.

## Decisão

1. **Cenários e massa ficam em texto, no git.** Nada de xlsx ou CSV como fonte: o versionamento, o diff do PR e o merge (conflito visível em vez de sobrescrita silenciosa) só funcionam com texto. Quem decide uma mudança é o PR, com `CODEOWNERS` nas especificações.
2. **Os cenários são especificações em português (Gherkin, `# language: pt`), executadas pelo Cucumber.** O que era uma aba de planilha vira um `.feature` com `Esquema do Cenário` e `Exemplos`: a mesma tabela, com as mesmas colunas, editada na IDE (o plugin de Gherkin alinha as colunas). O negócio lê e valida o próprio arquivo.
3. **Vocabulário fechado.** Cada campo tem uma coluna, e os termos (situações, ações, tipos de conta...) são validados por um `Vocabulario` antes de chegar à API. Termo errado ou registro de massa inexistente falha dizendo o arquivo, o cenário e os valores válidos.
4. **A massa é citada pela chave natural sempre que existir.** No Selic, conta, ISPB e código mais vencimento do título já identificam o registro: o cenário cita a conta, como o QA já faz. Onde a chave é gerada, vale a massa com IDs fixos (`R__massa_aceitacao.sql` e `BancoDaAceitacao`).
5. **Tudo roda no build**, com sandbox próprio (banco novo, JVM própria, em sequência, com sentinela). As mesmas especificações podem rodar contra homologação num job de CI parametrizado, que o QA dispara. Esse job substitui um sistema de testes implantado à parte.
6. **Mensageria:**
   - mensagens simples são conferidas por passos Java com um broker em Testcontainers;
   - se a validação de mensagens do catálogo SPB ficar pesada, entra o Citrus **dentro** do Cucumber (citrus-cucumber), ainda no `mvn verify`, sem sistema à parte.
7. **Cada nível com o seu volume.** As combinações exaustivas (cada tipo de conta × liquidante × transmissor) são da unidade, como a `TabelaDosFluxosTest`. A aceitação tem de 1 a 3 cenários por regra.

## Consequências

- **Normalização inicial:** as planilhas atuais são normalizadas uma vez para virar `Exemplos` (uma coluna por campo, código separado do texto, sem mescla). Depois disso, a planilha deixa de existir como fonte.
- **Cenários de vários passos com saldos:** viram um cenário por operação, com tabela de passos (`DataTable`), não um `Exemplos` plano. É o piloto natural, porque é o caso mais difícil.
- **Ferramenta dos QAs:** passam a usar o git e a IDE, com ganho de histórico e revisão. Exige treino curto: editar tabela Gherkin, abrir PR, ler o relatório do Cucumber.
- **Diff legível:** no PR, a mudança de um cenário aparece como a linha da tabela que mudou.
- **Rejeitados:**
  - planilha como fonte, mesmo convertida no build, porque não versiona;
  - CSV, porque o Excel estraga zeros à esquerda e datas ao salvar, e perde validações e abas;
  - Karate, porque as palavras-chave são fixas em inglês e os passos são a DSL dele;
  - um sistema de testes implantado à parte, porque separa o teste da versão do código e é mais um sistema para manter.
