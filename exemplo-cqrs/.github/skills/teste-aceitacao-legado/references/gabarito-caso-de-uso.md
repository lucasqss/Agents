# Gabarito do plano e do `caso-de-uso.md`

**Na fase A**, copie o bloco abaixo e preencha **todas** as seções, nesta ordem. Seção sem conteúdo leva "nenhum", nunca é apagada. Esse é o plano que você apresenta ao usuário.

**Na fase B**, grave o mesmo conteúdo em `src/test/resources/aceitacao/<capacidade>/caso-de-uso.md`, com as respostas do usuário. Troque a situação para `CONFIRMADO` e preencha as seções marcadas "(fase B)".

````markdown
# Caso de uso: <capacidade>

Situação: RASCUNHO, aguardando confirmação
Escopo: <endpoints incluídos>. Fora do escopo: <endpoints excluídos, ou "nenhum">.

## 1. Regras de negócio

| Regra | Descrição em linguagem de negócio | Origem | Trecho |
|---|---|---|---|
| R1 | | `Arquivo.java:00` | `código copiado` |

## 2. Contrato

| Operação | Entrada | Sucesso | Erros (status: quando) |
|---|---|---|---|
| `POST /...` | `{campo, campo}` | 201 `{id}` | 400: campo inválido; 404: ...; 409: ... |

Consulta usada para conferir o efeito: `GET /...`. Se não houver consulta por id, o item é procurado pelo id na listagem.

## 3. Modelos

### Checklist 1: contrato

| Item | Caso que cobre |
|---|---|
| campo `x` | C1 |
| status 409 | C7 |

### Checklist 2: dados de entrada
<tabela campo × válido × inválidos × limites, ou "não se aplica">

### Checklist 3: ciclo de vida
<tabela situação × ação, uma por tipo, sem célula vazia, ou "não se aplica">

### Checklist 4: condições combinadas
<tabela de condições, ou "não se aplica">

## 4. Casos

| Caso | Regra | Pré-condição | Entrada | Resultado esperado (um só) | Verificação do efeito |
|---|---|---|---|---|---|
| C1 | R1 | | | | |

## 5. Suspeitos (resposta obrigatória: fixar ou defeito)

| Suspeito | O que acontece hoje | Origem e trecho | Se for defeito, o correto seria | Resposta |
|---|---|---|---|---|
| S1 | | | | aguardando |

## 6. Dúvidas (nenhuma vira asserção)

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D1 | | | aguardando |

## 7. Rastreabilidade (fase B)

| Regra | Checklist | Teste | Casos |
|---|---|---|---|
| R1 | | `nomeDoMetodo` | 1 |

## 8. Comportamento atual suspeito (fase B)

Fixados como estão, por decisão do usuário. O teste tem `@DisplayName` começando por `[atual]`. Quando a produção for corrigida, ele falha de propósito e é atualizado junto.

| Suspeito | Teste |
|---|---|

## 9. Sessão exploratória (fase B)

Missão: <uma frase>

| Tentativa | Observado | Classificação: confirmado, defeito ou pergunta |
|---|---|---|

## 10. Defeitos (fase B)

| Regra ou suspeito | Esperado | Observado | Teste |
|---|---|---|---|
````

## Antes de apresentar o plano, confira

- [ ] Toda regra tem origem **e** trecho, copiados do `grep -n`.
- [ ] Todo item do checklist 1 tem um caso, ou virou dúvida.
- [ ] Toda tabela situação × ação está completa, e cada "recusa" tem caso.
- [ ] Todo caso tem **um** resultado esperado e uma verificação do efeito (ou "nada mudou", se recusado).
- [ ] Todo suspeito tem "se for defeito, o correto seria".
- [ ] Nenhum resultado esperado depende de suspeito ou dúvida sem resposta. Caso que depende de um deles cita o S ou o D na coluna de resultado.
