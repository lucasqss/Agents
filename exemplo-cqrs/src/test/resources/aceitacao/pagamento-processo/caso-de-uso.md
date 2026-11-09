# Caso de uso: pagamento de processo

Este documento é a fonte dos testes em `src/test/java/br/com/claudio/consorcio/aceitacao/pagamentoprocesso/`. Os testes conhecem apenas estas regras e o contrato HTTP publicado em `/q/openapi`.

## Regras de negócio

| Regra | Descrição |
|---|---|
| R1 | Um processo é aberto para uma cota, com tipo imobiliário ou serviço. |
| R2 | Serviço: aberto → em análise → aprovado → pago. |
| R3 | Imobiliário: aberto → em análise → aguardando vistoria → aprovado → pago. |
| R4 | Qualquer situação não final pode ser cancelada. Pago e cancelado são finais. |
| R5 | Ação fora da ordem é recusada (409) e a situação não muda. |
| R6 | Cada cota tem no máximo um processo em andamento. |
| R7 | Pagar registra a ordem na tesouraria, e o processo guarda o protocolo. |
| R8 | Com a tesouraria indisponível, o pagamento é recusado (503), o processo segue aprovado e sem protocolo, e pode ser pago depois. |
| R9 | Todo erro sai no mesmo formato: `{ "mensagem": ..., "campos": [...] }`. |

## Contrato usado

| Operação | Entrada | Resposta |
|---|---|---|
| `POST /processos` | `cotaId` e `tipo` (`IMOBILIARIO` ou `SERVICO`), obrigatórios | 201 e `id` |
| `POST /processos/{id}/iniciar-analise`, `concluir-analise`, `concluir-vistoria`, `pagar`, `cancelar` | nenhuma | `estado` |
| `GET /processos?tipo=&estado=&pagina=&tamanho=` | filtros opcionais | `itens[]` com `id`, `cotaId`, `cotaNumero`, `tipo`, `estado`, `protocoloPagamento`; `pagina`, `tamanho`, `total` |

Status: 400 entrada fora do formato, 404 inexistente, 409 conflito ou transição inválida, 503 sistema externo indisponível.

## Modelo: máquina de estados de negócio

| Situação | Serviço | Imobiliário | Sempre |
|---|---|---|---|
| ABERTO | iniciar-analise → EM_ANALISE | iniciar-analise → EM_ANALISE | cancelar → CANCELADO |
| EM_ANALISE | concluir-analise → APROVADO | concluir-analise → AGUARDANDO_VISTORIA | cancelar → CANCELADO |
| AGUARDANDO_VISTORIA | não existe | concluir-vistoria → APROVADO | cancelar → CANCELADO |
| APROVADO | pagar → PAGO | pagar → PAGO | cancelar → CANCELADO |
| PAGO, CANCELADO | final | final | nenhuma ação |

## Rastreabilidade

| Regra | Modelo e critério | Teste em `PagamentoProcessoAceitacaoTest` | Casos |
|---|---|---|---|
| R2, R3, R4 | Grafo: todos os caminhos até uma situação final | `fluxoChegaASituacaoFinal` | 9 |
| R5 | Grafo: ação inválida em cada situação | `acaoForaDaOrdemERecusada` | 41 |
| R1 | Particionamento com Base Choice | `escolhaBaseAbreProcesso`, `tipoImobiliarioAbreProcesso`, `cotaInexistente`, `cotaAusente`, `tipoInvalido`, `tipoAusente` | 6 |
| R6 | Tabela de decisão | `cotaComProcessoEmAndamentoNaoAbreOutro`, `cotaComProcessoFinalizadoAbreOutro`, `processoDeOutraCotaNaoImpede`, `pedidosSimultaneosAbremUmUnicoProcesso` | 13 |
| R7, R8 | Efeito observável pela listagem | `pagamentoGuardaProtocolo`, `tesourariaIndisponivel` | 2 |
| Contrato | Cobertura de terminais | `acaoEmProcessoInexistente`, `listagemFiltraPorTipoESituacao`, `listagemPaginada` | 7 |
| R9 | Suposição de erro | `corpoMalformado`, `corpoVazio` | 2 |

Total: 80 casos, executados em `*Test` e em `*IT`.

## Sessão exploratória

Missão: procurar respostas que contrariem as regras ou que as regras não definam. Apenas HTTP, contra o jar empacotado e PostgreSQL.

### Defeitos corrigidos

| Regra | Tentativa | Antes | Depois |
|---|---|---|---|
| R6 | 40 rodadas de 8 pedidos simultâneos de abertura na mesma cota | Nenhuma rodada criou exatamente 1 processo | Todas criaram exatamente 1; demais responderam 409 |
| R9 | `tipo` fora da lista, `cotaId` texto, JSON malformado | 400 com corpo vazio | 400 com `mensagem` |
| R9 | `POST /processos/abc/pagar`, id maior que o inteiro longo, `GET /processos?tamanho=abc` | 404 com corpo vazio | 404 com `mensagem` |
| R9 | `Content-Type: text/plain` em `POST /processos` | 415 com corpo vazio | 415 com `mensagem` |

Os casos `tipoInvalido` e `corpoMalformado` revelaram o defeito e não foram alterados para passar.

### Perguntas ao negócio: as regras não definem

| Tentativa | Observado hoje |
|---|---|
| `tamanho=0` ou negativo | Responde com `tamanho` 1 |
| `pagina=0` ou negativa | Responde 200 com itens |
| `tipo=XPTO` ou `estado=XPTO` na listagem | 200 e lista vazia |
| `tamanho=100000` | Responde 200 sem erro |
| Campo desconhecido no corpo de `POST /processos` | Ignora e abre o processo |

### Confirmado pela sessão

Processo e cota com id zero ou negativo respondem 404 com `mensagem`, e ação repetida responde 409.
