# Critérios: quantos casos bastam

Use os quatro checklists abaixo na etapa A4. Para cada regra, escolha o checklist que combina com ela. Um endpoint quase sempre usa o checklist 1 e mais um ou dois dos outros.

Todo caso tem quatro partes:

| Parte | Pergunta | Exemplo |
|---|---|---|
| Pré-condição | O que precisa existir antes? Sempre criado pelos endpoints | Processo aprovado |
| Entrada | Qual requisição? | `POST /processos/{id}/cancelar` |
| Resultado esperado | **Um só** status e os campos | `200`, `estado = CANCELADO` |
| Verificação do efeito | Qual **outro** endpoint confirma? | `GET /processos` mostra `CANCELADO` |

Status certo não prova o efeito. Todo caso que muda dados confere o efeito em outro endpoint. Todo caso recusado confere que **nada mudou**.

## Checklist 1: contrato (vale para todo endpoint)

Faça uma lista com cada item abaixo. Cada item precisa aparecer em ao menos um caso.

- [ ] cada campo da requisição;
- [ ] cada valor de cada enum (ex.: `tipo` = `SERVICO`, `IMOBILIARIO`);
- [ ] cada ação do caminho (ex.: `/iniciar-analise`, `/pagar`);
- [ ] cada status que o endpoint produz (ex.: 201, 400, 404, 409, 503);
- [ ] cada campo da resposta que o cliente usa.

Item sem caso possível vira dúvida.

## Checklist 2: dados de entrada (validação de campo)

Para cada campo validado, monte uma linha:

| Campo | Válido | Inválidos | Limites |
|---|---|---|---|
| `nome` (obrigatório, até 200) | texto de 10 | ausente, vazio, só espaços | 200 aceita, 201 recusa |
| `valor` (maior que zero) | 100 | 0, negativo, ausente | 0,01 aceita, 0 recusa |

- [ ] um caso com **todos os campos válidos**: é o caso-base;
- [ ] um caso para **cada inválido**, partindo do caso-base e mudando **só um campo**;
- [ ] os dois lados de **cada limite**: o último aceito e o primeiro recusado;
- [ ] no 400, conferir **qual campo** foi recusado, se a resposta informar.

DTO usado por vários endpoints: os limites são testados uma vez. Nos outros endpoints, basta um inválido por campo.

## Checklist 3: ciclo de vida (situação, status, estado)

Monte a tabela **situação × ação** com **todas** as situações e **todas** as ações. Cada célula recebe o destino ou "recusa".

| Situação | iniciar | concluir | cancelar |
|---|---|---|---|
| ABERTO | EM_ANALISE | recusa | CANCELADO |
| EM_ANALISE | recusa | APROVADO | CANCELADO |
| APROVADO | recusa | recusa | CANCELADO |
| CANCELADO | recusa | recusa | recusa |

- [ ] a tabela não tem célula vazia;
- [ ] cada célula com destino vira um caso que confere a nova situação;
- [ ] cada célula "recusa" vira um caso que confere o status de recusa **e** que a situação não mudou;
- [ ] cada situação final é alcançada por um caminho completo, do início ao fim;
- [ ] se o caminho muda por tipo (ex.: imobiliário tem vistoria), faça **uma tabela por tipo**.

Muitas células: gere os casos com `@ParameterizedTest` e `@MethodSource` a partir da tabela, em vez de escrever um método por célula.

## Checklist 4: regra com condições combinadas

Monte uma linha por condição. Em cada linha, só aquela condição muda em relação ao caso-base.

| Cota existe? | Já tem processo em andamento? | Resultado | O que o caso prova |
|---|---|---|---|
| sim | não | 201 | caso-base |
| não | não | 404 | a existência da cota decide |
| sim | sim | 409 | o processo em andamento decide |

- [ ] o caso-base;
- [ ] cada condição muda o resultado sozinha em algum caso;
- [ ] "em andamento" e "finalizado" são testados dos dois lados (ex.: depois de pago, pode abrir de novo?);
- [ ] o dado de outro agregado não interfere (ex.: processo em andamento de **outra** cota).

## O que fica para a exploração

Corpo malformado, tipo errado, ação repetida, pedidos simultâneos, sistema externo fora. O que o código define vira caso. O que ele não define vira dúvida.
