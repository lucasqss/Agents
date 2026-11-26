# Como preencher a massa e as tabelas de cenários

> As colunas e termos abaixo são os do `exemplo-testes` (processo de pagamento). Cada projeto tem os seus, no mesmo formato: a massa em `cucumber/massa/*.md` e o vocabulário em `cucumber/passos/Vocabulario.java`.

## A massa: `cucumber/massa/processos.md`

Uma tabela Markdown. O GitHub a mostra formatada no PR, e a IDE alinha as colunas. O texto acima da tabela explica a massa a quem preenche.

| Coluna | O que é |
|---|---|
| `cota` | A chave natural, única, até 30 caracteres. `COTA-SENTINELA` é reservada |
| `tipo` | serviço ou imobiliário |
| `valor` | em reais: `50.000,01` |
| `situação` | Aberto, Em análise, Pendente de documento, Aguardando vistoria, Aguardando alçada, Aprovado, Pago, Reprovado, Cancelado |
| `observação` | livre, para quem lê; não vai para o banco. Diga para qual cenário a linha existe |

- **Uma linha por situação de partida, e por cenário:** dois cenários na mesma cota dependeriam da ordem.
- **Linha nova em qualquer posição:** os cenários citam a cota, não o número.
- O build valida o arquivo inteiro antes de subir a aplicação, e a falha diz a linha.

## As "abas": um `.feature` por família

Cada arquivo começa com `# language: pt`, uma `Funcionalidade:` com a regra explicada como o analista explicaria, e um parágrafo "Como preencher". O prefixo numérico (`01-`, `02-`) fixa a ordem de execução e, com ela, os números gerados pelos cenários que abrem processo.

Toda tabela `Exemplos` começa com `cenário` (o número, ex.: `4.2`), `regra` (`R4`) e `descrição` (o resultado em uma frase). Os três formam o nome do teste nos relatórios.

**Célula vazia = não conferir.** A exceção é `motivo`: vazio manda a ação sem motivo.

### `01-abertura.feature`

| Coluna | Valores |
|---|---|
| `tipo`, `cota`, `valor` | o que se abre |
| `resultado` | aceita ou recusada |
| `número` | o número que o processo recebe (só quando ele é o ponto do cenário: 1000 é o primeiro) |
| `situação` | a situação depois da abertura |

### `02-acoes.feature`: uma ação sobre um processo da massa

| Coluna | Valores |
|---|---|
| `cota` | o processo da massa |
| `quem` | o usuário que pede |
| `ação` | iniciar a análise, solicitar documento, receber o documento, concluir a análise, concluir a vistoria, aprovar na alçada, devolver para análise, reprovar, pagar, cancelar |
| `motivo` | texto, ou vazio |
| `resultado` | aceita ou recusada |
| `situação` | a situação gravada depois da ação |
| `mensagem` | a mensagem que a pessoa vê na recusa |
| `ações disponíveis` | separadas por vírgula, ou `nenhuma` |

### `03-fluxos.feature`: vários passos sobre o mesmo processo

Um `Cenário` por fluxo, que parte de `Dado o processo da cota "X" da massa` ou de `Dado um processo novo de serviço, da cota "X", com valor de 30.000,00`. Os passos vêm numa tabela, com as mesmas colunas da aba de ações:

```gherkin
Quando os passos abaixo são executados em ordem:
  | quem  | ação                | motivo | resultado | situação          |
  | vitor | concluir a vistoria |        | aceita    | Aguardando alçada |
  | carla | aprovar na alçada   |        | aceita    | Aprovado          |
Então o histórico tem 2 registros
E o último registro do histórico é:
  | ação | aprovar na alçada |
  | quem | carla             |
```

Uma falha diz o passo: `passo 2 (aprovar na alçada): ...`.

## Coluna ou frase nova

Só quando as tabelas não dão conta:

1. Escreva a coluna ou a frase e rode: o Cucumber diz que o passo não tem definição.
2. Acrescente o método em `PassosDoProcesso` com `@Dado`, `@Quando`, `@Entao` ou `@E` (sem acento no Java: o ErrorProne não aceita acento em identificador; no `.feature` continua "Então").
3. Converta os termos pelo `Vocabulario`; trate a célula vazia como "não conferir".
4. Só HTTP; o efeito é conferido por outro endpoint.

## Erros comuns

| Sintoma | Causa |
|---|---|
| `Massa inválida: processos.md, linha 30: Termo de situação desconhecido: 'Aguardando alcada'` | Termo diferente do vocabulário (aqui, sem cedilha). A mensagem lista os válidos |
| `A cota 'COTA-999' não está em cucumber/massa/processos.md` | O cenário cita uma cota que não existe na massa |
| `A cota COTA-003 da massa já é usada pelo cenário ...` | Duas linhas de `Exemplos` (ou dois cenários) na mesma cota. Crie outra linha na massa |
| `situação gravada do processo da cota X, que a massa diz (...)` | A massa diz uma situação e o banco tem outra: o arquivo mudou ou foi a cota errada |
| `expected: <Aprovado> but was: <Aguardando alçada>` | O sistema fez diferente do que a tabela espera: defeito, ou esperado a rever com o analista |
| Cenário `UNDEFINED` | A frase não casa com nenhum passo: confira as aspas e as vírgulas |
| Número do processo aberto diferente do esperado | Um cenário novo, que abre processo, entrou antes deste na ordem dos arquivos |
