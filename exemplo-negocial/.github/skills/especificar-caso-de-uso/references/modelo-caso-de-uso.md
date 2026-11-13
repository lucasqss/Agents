# Modelo do `caso-de-uso.md`

Copie o bloco e preencha todas as seções, nesta ordem. Seção sem conteúdo leva "nenhum", nunca é apagada. As seções de contrato, modelo e rastreabilidade (e a sessão exploratória) são acrescentadas depois, ao mesmo arquivo, pela implementação e pela skill `teste-aceitacao-modelos`. Rascunho de mudança de caso de uso confirmado: veja [mudar um caso de uso confirmado](mudar-caso-de-uso-confirmado.md).

**Origem.** Regra e linha do quadro de papéis dizem de onde vêm, numa coluna "Origem". Valores: `dono, <data>` (o dono disse; se um intermediário relatou, `dono, <data>, via <quem>`), ou `proposta, Dn` (veio de quem escreveu ou intermediou; Dn é a dúvida aberta com a pergunta exata). Quando a dúvida é respondida, a origem passa a `dono, <data> (Dn)`; se foi por decorrência, `decorrência de <Rn ou Dn>`.

````markdown
# Caso de uso: <capacidade>

Situação: RASCUNHO, aguardando confirmação | CONFIRMADO em <data> por <dono>

## Objetivo

<Uma ou duas frases: que problema resolve e para quem.>

## Atores

| Ator | O que faz nesta capacidade |
|---|---|
| <papel> | |
| Rotina automática de <o quê> | <O que faz, quando e com que resultado. Autor "sistema".> |

Rotina automática é ator que não é pessoa: age por tempo, não por pedido de alguém. Sistema externo que só responde quando perguntado não é ator: aparece em "Sistemas externos".

Quem pode executar cada ação:

| Ação | Papel | Origem |
|---|---|---|
| | | |

Uma pessoa pode ter mais de um papel? <resposta ou dúvida Dn>. Há ações que exigem pessoas diferentes? <Quais, e a regra Rn que o diz; ou "nenhuma", ou dúvida Dn.>

## Vocabulário

| Termo | Significado |
|---|---|
| | |

## Dados

| Dado | Obrigatório | Formato ou faixa | Observação |
|---|---|---|---|
| | | | |

## Regras de negócio

| Regra | Descrição | Exemplo que cumpre | Exemplo que viola | Origem |
|---|---|---|---|---|
| R1 | | | | |

Valor ainda em dúvida (limite, prazo) aparece nos exemplos como ilustrativo, com a dúvida: "alçada de R$ 50.000,00 (ilustrativa, D1)".

## Ciclo de vida

Situações: <lista, marcando as finais>.

Cada célula é uma destas: "vai para <situação>" (pode ter condição: "vai para X se a condição, senão Y"), "recusada", "não se aplica" ou "dúvida Dn".

Regra geral: "ação fora da ordem é recusada" (Rn). Com ela dita pelo dono, célula que ninguém mencionou é "recusada" e não vira dúvida; só vira dúvida onde há ambiguidade real. Se o dono não disse a regra geral, ela é a primeira dúvida.

| Situação \ Ação | <ação 1> | <ação 2> | rotina: <resultado> |
|---|---|---|---|
| <situação 1> | vai para <situação> | recusada | não se aplica |

Uma tabela por tipo, se o fluxo muda por tipo. Cada tipo tem a **tabela completa**, com as colunas e situações daquele tipo, sem remissão a outra tabela, mesmo quando só uma ação ou situação existe num tipo. "Não se aplica" se não há ciclo de vida.

Colunas "rotina: <resultado>" são resultados que a rotina automática aplica por tempo; **não são pedidos de pessoa**. Só fazem algo na situação em que a rotina age; nas demais, "não se aplica". Regra de prazo ou intervalo vira regra com origem e, se o valor não foi decidido, dúvida.

## Sistemas externos

| Sistema | O que pedimos | O que devolve | Se não responder ou recusar |
|---|---|---|---|
| | | | |

## Recusas

| Situação | O que a pessoa é informada | O que acontece com o dado |
|---|---|---|
| | | |

Inclui pedidos simultâneos e pedido repetido.

## Consultas

| O que a pessoa precisa ver | Filtros | Canal |
|---|---|---|
| | | |

## Fora do escopo

- <o que foi citado e adiado>

## Decisões de solução citadas pelo dono

Fora das regras: não são requisito de negócio e não geram teste. Servem a quem for implementar, que as lê antes de escolher a solução.

| Quando | O que o dono disse, nas palavras dele | Regra relacionada |
|---|---|---|
| <data> | | Rn |

"Nenhuma" se o dono não citou solução.

## Dúvidas

Nenhuma vira regra, teste ou código enquanto estiver sem resposta. Dúvida respondida não é apagada nem renumerada: passa para "Respondidas".

### Abertas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D1 | | | aguardando |

### Respondidas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D2 | | | RESPONDIDA em <data>: <resposta do dono> |
| D3 | | | RESPONDIDA em <data>, POR DECORRÊNCIA de <Rn ou Dn>, não por resposta direta do dono: <como decorre>. O dono pode contestar. |
````
