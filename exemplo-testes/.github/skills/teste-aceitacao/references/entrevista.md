# Roteiro de entrevista (aceitação)

Poucas perguntas por vez. O dev traz o que o analista definiu; o que nenhum dos dois souber vira dúvida para o analista.

## Os cenários

- Quais cenários o analista definiu para esta capacidade? Para cada um: o que a pessoa faz, em que ordem, e o que ela vê no fim.
- Qual é a regra por trás de cada cenário (R1, R2...)? Há cenário sem regra, ou regra sem cenário?
- Quais são positivos (a ação é aceita) e quais negativos (a ação é recusada)? Para os negativos, o que a pessoa vê?
- Algum cenário tem vários passos (criar, aprovar, cancelar)? O que deve ser conferido depois de cada passo?

## A massa

- Que cadastro os cenários precisam (clientes, contas, produtos)? Quais atributos importam para o resultado (limite, situação, tipo)?
- Que sistemas externos aparecem? Para cada um, quais respostas os cenários precisam (aprova, recusa, fora do ar)?
- Há valores que se repetem em muitos cenários e merecem uma chave de massa própria ("cliente padrão")?

## O que fica em outro nível

- As variações exaustivas da regra (cada limite, cada situação) já estão na unidade? Se não, elas vão para a `teste-unidade`, não para `cenarios.yaml`.
- Erros técnicos (formato, `401`/`403`, sistema fora do ar com timeout) estão no componente?

## Encerramento

Mostre os cenários e a massa propostos e pergunte: "O analista reconhece estes cenários? Falta algum?"
