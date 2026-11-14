# Técnicas e critérios de parada

Não é preciso conhecer os livros. Este texto explica cada técnica e mostra quando usar. No teste de unidade, a "entrada" é a chamada de método e o "efeito" é o estado do objeto (situação, histórico, valores), conferido logo depois. Os nomes em inglês são os termos usados em *Introduction to Software Testing* (Ammann e Offutt) e *The Art of Software Testing* (Myers, Sandler e Badgett), para quem quiser se aprofundar.

## A ideia

Um **modelo** é um resumo do comportamento de negócio. Um **critério** diz quando já testamos o suficiente desse modelo. O caminho é sempre:

**requisito → modelo → requisitos de teste, pelo critério → casos de teste → automação → execução → avaliação**

Assim, a pergunta "testamos o suficiente?" tem resposta objetiva. Os dois livros dão a base: teste é procurar erros, e entradas inválidas merecem tanta atenção quanto as válidas.

## Anatomia de um caso

| Parte | Pergunta | Exemplo |
|---|---|---|
| Pré-condição | O que precisa existir antes? | Pedido aprovado, montado pela fábrica do domínio |
| Entrada | O que acontece? | `pedido.cancelar(autor, motivo)` |
| Resultado esperado | O que a regra manda acontecer? **Um só.** | situação `CANCELADO` |
| Verificação do efeito | O que mais mudou, ou não mudou? | histórico ganhou uma entrada com o motivo |

**Por que só o retorno não basta (modelo RIPR):** para um teste revelar um defeito, o caso precisa **alcançar** o ponto errado, o erro precisa **contaminar** o estado, a contaminação precisa **propagar** até algo visível, e o teste precisa **olhar** para esse lugar. Conferir só o retorno deixa de olhar para boa parte do que foi afetado.

## Particionamento do espaço de entrada

Divida cada entrada em **classes** em que o sistema deve se comportar igual, segundo o **negócio**. Basta testar um representante de cada classe (Myers: particionamento de equivalência).

| Entrada | Classes válidas | Classes inválidas | Limites |
|---|---|---|---|
| Valor (maior que zero) | positivo | zero, negativo, ausente, texto | 0,01 e 0 |
| Nome (até 200) | 1 a 200 caracteres | vazio, ausente, 201 | 1, 200 e 201 |

**Valores-limite:** erros se concentram nas bordas. Teste exatamente o limite e o valor logo depois.

**Base Choice:** escolha o caso mais comum como base e varie **uma** característica por vez. Se o teste falhar, você sabe qual delas causou a falha.

| Caso | Cliente | Tipo |
|---|---|---|
| Base | existente | NORMAL |
| Variação | inexistente | NORMAL |
| Variação | ausente | NORMAL |
| Variação | existente | EXPRESSO |
| Variação | existente | inválido |

Use *Pair-Wise* apenas se o requisito indicar que as características interagem.

## Máquina de estados de negócio

Use quando o requisito descreve um ciclo de vida. Os estados e as ações vêm **do requisito**. Desenhe uma tabela:

| Situação | aprovar | enviar | cancelar |
|---|---|---|---|
| ABERTO | APROVADO | recusar | CANCELADO |
| APROVADO | recusar | ENVIADO | CANCELADO |
| ENVIADO | recusar | recusar | recusar |
| CANCELADO | recusar | recusar | recusar |

Mínimo:

- **Todas as transições** (*Edge Coverage*): cada célula com destino é exercitada.
- **Cada estado final** é alcançado por ao menos um caminho.
- **Cada ação inválida em cada estado:** cada "recusar" da tabela é um caso, que também confere que a situação não mudou.

Em unidade a matriz inteira custa milissegundos: use `@ParameterizedTest` com `@EnumSource`/`@MethodSource` sobre situações e ações. Cada recusa confere a exceção **e** que a situação e o histórico não mudaram. Para "vale em qualquer caminho" (estado final sem saída, nada é pago sem aprovação), complemente com um teste de propriedade que anda por caminhos gerados ([propriedades](propriedades.md)).

Se o grafo for pequeno, cubra os **caminhos primos** (*Prime Path Coverage*), que em um fluxo sem ciclos são todos os caminhos do início a cada estado final.

## Tabela de decisão

Use quando uma regra combina condições (Myers: grafo de causa e efeito). Cada condição precisa **decidir sozinha** o resultado em algum caso (*Correlated Active Clause Coverage*):

| Tem saldo? | Conta ativa? | Resultado | O que este caso prova |
|---|---|---|---|
| sim | sim | aprova | caso-base |
| não | sim | recusa | o saldo decide |
| sim | não | recusa | a situação da conta decide |

## Suposição de erro

Liste o que costuma dar errado, por experiência (Myers: *error guessing*): nulo, vazio, zero, negativo, ação repetida, lista sem itens, mesma pessoa em dois papéis. O que o dev confirmou na entrevista vira caso; o resto vira dúvida no plano.

## Rastreabilidade

No plano (e no `caso-de-uso.md`, se existir):

| Regra | Técnica e critério | Teste | Casos |
|---|---|---|---|
| R1 | Máquina de estados: todas as transições | `PedidoFluxoTest.transicoes` | 9 |

Regra sem teste é lacuna. Teste sem regra é suposição.
