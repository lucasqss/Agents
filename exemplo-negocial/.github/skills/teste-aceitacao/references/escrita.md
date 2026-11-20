# Como escrever as especificações

## Forma

- Primeira linha `# language: pt`. Palavras-chave: `Funcionalidade`, `Contexto`, `Cenário`, `Esquema do Cenário`, `Exemplos`, `Dado`, `Quando`, `Então`, `E`, `Mas`.
- **Um arquivo por regra** (ou por grupo pequeno de regras), com prefixo numérico: o prefixo fixa a ordem de execução, e com ela os IDs gerados.
- `Funcionalidade:` com o nome da regra e, abaixo, duas ou três linhas que a explicam como o analista explicaria.
- Tag com o número da regra (`@R4`) no arquivo ou no cenário: rastreabilidade entre requisito e teste.
- Nome do cenário começa pelo número (`4.2 ...`) e diz o resultado: "Valor acima da alçada aguarda a alçada", não "Teste de alçada 2".
- Combinações da mesma regra: `Esquema do Cenário` com `Exemplos`. O negócio lê a tabela; cada linha vira um cenário.

## Vocabulário disponível

> O vocabulário abaixo é o do `exemplo-testes` (processo de pagamento). Cada projeto tem o seu, no mesmo formato, em `cucumber/passos/Vocabulario.java`.

Os termos ficam em `passos/Vocabulario.java`:

- **Situações** (sempre entre aspas): "Aberto", "Em análise", "Pendente de documento", "Aguardando vistoria", "Aguardando alçada", "Aprovado", "Pago", "Reprovado", "Cancelado".
- **Ações:** iniciar a análise, solicitar documento, receber o documento, concluir a análise, concluir a vistoria, aprovar na alçada, devolver para análise, reprovar, pagar, cancelar.
- **Tipos:** serviço, imobiliário.
- **Valores** em reais como o negócio escreve: `50.000,01`.

| Frase | Para quê |
|---|---|
| `Dado o processo 6 da massa, de serviço, na situação "Em análise"` (`, com valor de 50.000,01`) | Partir de uma situação pronta da massa; confere a pré-condição |
| `Dado um processo novo de serviço, da cota "COTA-101", com valor de 30.000,00` | Partir do começo, abrindo um processo |
| `Quando alguém abre um processo de serviço, da cota "X", com valor de 0,00` | A abertura é o que se testa |
| `Quando "ana" concluir a análise` | Ação sem motivo |
| `Quando "ana" reprovar com o motivo "renda incompatível"` | Ação com motivo |
| `Então o processo é aberto com o número 1000` | Abertura aceita (e o número, quando ele é o ponto do cenário) |
| `Então a abertura é recusada` | Abertura recusada |
| `Então o processo fica "Aprovado"` | Ação aceita, e a situação gravada |
| `Então a ação é recusada` (`com a mensagem "..."`) | Ação recusada, e a mensagem que a pessoa vê |
| `E o processo continua "Em análise"` | Nada mudou |
| `E as ações disponíveis são:` + uma ação por linha | Os botões que a pessoa vê |
| `E nenhuma ação está disponível` | Situação final |
| `E o histórico tem 3 registros` | Quantas transições houve |
| `E o último registro do histórico é:` + tabela `ação`, `quem`, `de`, `para`, `motivo` | O que ficou registrado |

## Frase nova

Só quando o vocabulário não der conta, e escrita como o negócio fala:

1. Escreva a frase na especificação e rode: o Cucumber diz que ela não tem definição.
2. Acrescente o método em `PassosDoProcesso` com `@Dado`, `@Quando`, `@Entao` ou `@E` (sem acento no Java: o ErrorProne não aceita acento em identificador; no `.feature` continua "Então").
3. Use os tipos do vocabulário (`{situacao}`, `{acao}`, `{tipo}`, `{valor}`) para que termo errado falhe com a lista dos válidos.
4. Só HTTP; o efeito é conferido por outro endpoint.

## Erros comuns

| Sintoma | Causa |
|---|---|
| `Termo de situação desconhecido: 'Aguardando alcada'` | Termo escrito diferente do vocabulário (aqui, sem cedilha). A mensagem lista os válidos |
| Cenário `UNDEFINED` | A frase não casa com nenhum passo: confira vírgulas e aspas, ou crie a frase |
| `O processo N da massa já é usado pelo cenário ...` | Dois cenários (ou duas linhas de `Exemplos`) sobre o mesmo registro. Crie outro na massa |
| `tipo do processo N na massa` / `situação do processo N na massa` | A massa SQL não está como o `Dado` diz: o SQL mudou ou o cenário cita o ID errado |
| Número do processo aberto diferente do esperado | Um cenário novo, que abre processo, entrou antes deste na ordem dos arquivos |
