# ADR 002: Pagamento como saga orquestrada: solicitar só grava, uma rotina agendada fala com a tesouraria

**Status:** Aceito
**Data:** 13 de novembro de 2026
**Decidido por:** dono do produto (decisão D15 do caso de uso `pagamento-processo`)

## 1. Contexto

Pagar um processo grava no nosso banco e registra uma ordem na tesouraria, que é um sistema externo: o banco desfaz o que gravou, a tesouraria não. A skill `arquiteto-padrao` ([transação e sistema externo](../../.github/skills/arquiteto-padrao/references/transacao-e-sistema-externo.md)) pede que a escolha entre as três formas fique em ADR. O primeiro desenho (chamada dentro da transação, com `POST /processos/{id}/pagar`) prendia a conexão e o bloqueio enquanto a tesouraria respondia, recusava o pagamento quando ela estava fora do ar (503) e dependia de ela estar disponível no instante do pedido. O dono pediu: "criar estado intermediário e só sair dele mediante evento temporal", seguindo a saga "Fairy Tale".

## 2. Decisão

Saga **orquestrada** (Fairy Tale Saga: orquestração, chamada síncrona à tesouraria, consistência eventual), na forma 2 da skill: estado intermediário, três passos, cada um com a sua transação ou sem nenhuma.

1. **Solicitar** (`POST /processos/{id}/solicitar-pagamento`, `SolicitarPagamentoProcessoUseCase`, `@Transactional`): só leva APROVADO a `PAGAMENTO_SOLICITADO` e guarda a **identificação da solicitação** (UUID sorteado no caso de uso, Value Object `IdentificacaoSolicitacao`; coluna `identificacao_solicitacao`, migration V8). Nada vai à tesouraria.
2. **Chamar** (`ExecutarRotinaDePagamentoUseCase`, **sem** transação): chamado por `entrypoint/batch/processo/RotinaDePagamentoJob`, `@Scheduled(every = "{pagamento.rotina.intervalo}", concurrentExecution = SKIP)`, padrão 5 minutos. Para cada processo em `PAGAMENTO_SOLICITADO`, lê o processo, chama o `TesourariaGateway` com a identificação como `requestId` (chave de idempotência) e entrega o resultado ao passo 3. Falha em um processo é registrada em log e não impede os demais.
3. **Aplicar** (`AplicarResultadoPagamentoUseCase`, `@Transactional`): relê o processo **com bloqueio** e aplica o resultado só se ele ainda está em `PAGAMENTO_SOLICITADO` com a **mesma identificação**; senão não faz nada.

O fluxo (tabela de transições): `SOLICITAR_PAGAMENTO` (APROVADO → PAGAMENTO_SOLICITADO) é da pessoa; `CONFIRMAR_PAGAMENTO` (→ PAGO), `RECUSAR_PAGAMENTO` (→ APROVADO) e `TENTAR_PAGAMENTO` (PAGAMENTO_SOLICITADO → PAGAMENTO_SOLICITADO) são só da rotina: não aparecem em `GET /processos/{id}/acoes` nem passam pelo executar genérico. `PAGAMENTO_SOLICITADO` não tem `CANCELAR`: é uma exceção declarada na tabela (`semCancelamentoEm`), não um `if`. Só a rotina tira o processo dali. Não existe endpoint para a tesouraria nos avisar nem para disparar a rotina. `pagar` e `PagarProcessoUseCase` deixam de existir.

O `TesourariaGateway` devolve sempre um `RespostaDaTesouraria` (record próprio do gateway: confirmada, recusada, sem resposta, incoerente) e nunca lança exceção: 5xx, timeout de 2 segundos e circuito aberto são "sem resposta". O caso de uso traduz para o `ResultadoPagamento` do domínio, que decide o que é incoerente: confirmação sem protocolo e recusa sem motivo não são aplicadas e viram `TENTAR_PAGAMENTO` (R31); `ALREADY_PROCESSED` com protocolo vale como confirmação. Não há `@Retry`: quem repete é a rotina, no próximo intervalo, com a mesma identificação. O **circuit breaker** (`@CircuitBreaker` no client) fica: sem limite de tentativas e sem aviso, uma tesouraria fora do ar faria cada rodada gastar 2 segundos por processo esperando; com o circuito aberto as chamadas falham na hora e o resto da rodada é rápido.

Os testes de integração desligam o agendamento (`%test.pagamento.rotina.intervalo=off`) e chamam o caso de uso; os de aceitação ligam `1s` pelo `TesourariaStubResource`.

## 3. Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Chamar a tesouraria dentro da requisição de pagar (forma 1, o desenho anterior) | Prende a conexão e o bloqueio enquanto a tesouraria responde; o pedido falha quando ela está fora do ar; um pico de pedidos esgota as conexões |
| Callback: a tesouraria nos chama com o resultado | A tesouraria só responde quando a rotina pergunta (decisão do dono); um endpoint de entrada exigiria autenticação, contrato e tratamento de mensagem repetida ou fora de ordem |
| Fila ou outbox (forma 3) | Precisa de tabela, processo de envio e decisão de plataforma que o padrão ainda não tem; a tesouraria responde na hora e a latência de 5 minutos já é aceita pelo negócio, então o estado intermediário com rotina basta |

## 4. Consequências

- **Consistência eventual:** entre solicitar e a rotina o processo fica em `PAGAMENTO_SOLICITADO`, sem protocolo, e a cota segue indisponível; o crédito só cai em `PAGO`. O pagamento leva até um intervalo (5 minutos) mais o tempo da tesouraria.
- **Histórico cresce a cada tentativa:** cada rodada sem resultado grava uma entrada `TENTAR_PAGAMENTO`. Não há limite de tentativas nem aviso a ninguém, por decisão do dono; um processo parado cresce o histórico a cada 5 minutos até a tesouraria responder.
- **Cada nova solicitação tem identificação nova;** as tentativas da mesma solicitação repetem a dela. A recusa descarta a identificação.
- Nada fica pela metade sem poder ser retomado, e a rotina é a retomada: ver abaixo.
- Obrigatório: a dependência `io.quarkus:quarkus-scheduler`; migration V8; `pagamento.rotina.intervalo` configurável; teste de concorrência com duas execuções da rotina.
- Várias instâncias: o job roda em cada uma (o `SKIP` só vale dentro de uma instância). É seguro: todas chamam com a mesma identificação e só quem gravar primeiro aplica o resultado, porque o passo 3 bloqueia e confere a identificação. O custo é chamadas repetidas à tesouraria, que devolve `ALREADY_PROCESSED`.

### O que acontece em cada falha

| Falha | Efeito | Como se resolve |
|---|---|---|
| Tesouraria fora do ar, lenta (acima de 2 s) ou circuito aberto | Resposta "sem resposta": o processo continua em `PAGAMENTO_SOLICITADO` e a tentativa vai ao histórico | Próxima rodada, mesma identificação |
| Resposta incoerente (confirmação sem protocolo, recusa sem motivo, status desconhecido) | Não é aplicada; vai ao histórico como tentativa | Próxima rodada |
| Queda do nosso lado entre a chamada e a gravação (a tesouraria abriu a ordem e nós não gravamos) | O processo segue em `PAGAMENTO_SOLICITADO`, sem protocolo | Próxima rodada, mesma identificação: a tesouraria responde `ALREADY_PROCESSED` com o protocolo, que vale como confirmação; não abre segunda ordem |
| Duas execuções da rotina ao mesmo tempo sobre o mesmo processo | As duas chamam a tesouraria com a mesma identificação | A que gravar primeiro aplica (um `CONFIRMAR_PAGAMENTO`); a outra relê com bloqueio, vê que o processo já saiu do estado e não faz nada |
| Falha ao gravar em um processo | Registrada em log; os outros processos seguem | Próxima rodada |
| Recusa da tesouraria | Volta a APROVADO com o motivo no histórico; pode ser solicitado de novo, com identificação nova | Nova solicitação da pessoa |

## 5. Como é verificado

- ArchUnit: o job só chama caso de uso; só caso de uso chama o gateway; `@Transactional` só em `application`; `@Retry` e `@CircuitBreaker` só no gateway.
- JUnit puro do fluxo e da raiz (`FluxoProcessoTest`, `PagamentoComoSagaTest`), do gateway (`TesourariaGatewayTest`) e da rotina (`ExecutarRotinaDePagamentoUseCaseTest`).
- Integração: `RotinaDePagamentoTest` (confirmou, recusou, indisponível, queda entre chamada e gravação, incoerente, circuito aberto) e `ConcorrenciaProcessoTest` (duas execuções da rotina aplicam o resultado uma vez, com a mesma identificação).
- Aceitação: `PagamentoProcessoAceitacaoTest`, com a rotina em 1 segundo e a tesouraria simulada por WireMock.
