# Modelo do `caso-de-uso.md`

Copie o bloco e preencha todas as seções, nesta ordem. Seção sem conteúdo leva "nenhum", nunca é apagada. As seções de contrato, modelo e rastreabilidade são acrescentadas depois, ao mesmo arquivo, pela implementação e pela skill `teste-aceitacao-modelos`.

````markdown
# Caso de uso: <capacidade>

Situação: RASCUNHO, aguardando confirmação | CONFIRMADO em <data> por <dono>

## Objetivo

<Uma ou duas frases: que problema resolve e para quem.>

## Atores

| Ator | O que faz nesta capacidade |
|---|---|
| | |

## Vocabulário

| Termo | Significado |
|---|---|
| | |

## Dados

| Dado | Obrigatório | Formato ou faixa | Observação |
|---|---|---|---|
| | | | |

## Regras de negócio

| Regra | Descrição | Exemplo que cumpre | Exemplo que viola |
|---|---|---|---|
| R1 | | | |

## Ciclo de vida

Situações: <lista, marcando as finais>.

| Situação \ Ação | <ação 1> | <ação 2> |
|---|---|---|
| <situação 1> | vai para <situação> | recusada |

Uma tabela por tipo, se o fluxo muda por tipo. "Não se aplica" se não há ciclo de vida.

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

## Dúvidas

Nenhuma vira regra, teste ou código enquanto estiver sem resposta.

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D1 | | | aguardando |
````
