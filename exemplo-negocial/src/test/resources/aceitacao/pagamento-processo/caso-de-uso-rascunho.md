# Caso de uso: pagamento de processo

Situação: RASCUNHO, aguardando confirmação

Este rascunho é a MUDANÇA do caso de uso confirmado (`caso-de-uso.md`, na mesma pasta), escrito por inteiro, não só a diferença. O `caso-de-uso.md` continua sendo a fonte dos testes até o dono confirmar. Origem: o pedido do dono (quatro frentes: mais caminhos no fluxo, valor e favorecidos, pagamento em duas etapas, alçada e autoria), uma proposta de partida e as decisões que o dono tomou em 13/11/2026. **Decisões do dono em 13/11/2026:** (1) R6 continua: a cota fica indisponível para novo processo até o processo chegar a uma situação final, para coibir fraudes; (2) o pagamento é feito por uma rotina automática: solicitar leva a "pagamento solicitado" e só a rotina tira o processo dessa situação; (3) valor e favorecidos podem ser alterados: livremente pelo cliente enquanto o processo está aberto e, depois, só pelo backoffice, em análise, com a concordância do cliente. As regras que vêm dessas decisões estão marcadas "decisão do dono"; R14 decorre delas (R6 e R13), e o dono pode contestar. As demais regras marcadas "(nova)" ou "(alterada)" continuam proposta até a confirmação, e as demais dúvidas aguardam a revisão do dono na seção Dúvidas. O caso de uso confirmado está desatualizado em relação ao sistema em dois pontos (devolução para análise e histórico de ações); este rascunho os incorpora como R23 e R19. Valores de alçada e de intervalo da rotina nos exemplos são ilustrativos (D1).

## Objetivo

Permitir que a pessoa da área acompanhe o pagamento de um processo de uma cota, do início ao fim: análise (com pendência de documento, reprovação e devolução), aprovação por uma segunda pessoa quando o valor é alto, e pagamento em duas etapas: a pessoa solicita e uma rotina automática pede o pagamento à tesouraria. O valor do processo é limitado ao crédito disponível da cota e pode ser dividido entre favorecidos; o cliente os altera enquanto o processo está aberto e, depois, só o backoffice altera, com a concordância do cliente. Cada cota fica indisponível para novo processo até o processo chegar a uma situação final. Tudo fica registrado: quem fez, quando e por quê.

## Atores

| Ator | O que faz nesta capacidade |
|---|---|
| Cliente | Altera valor e favorecidos enquanto o processo está aberto; concorda ou discorda (com motivo) da alteração feita pelo backoffice; quem abre o processo: D10 |
| Backoffice (analista) (D27) | Analisa, pede e recebe documento, reprova, conclui a análise e a vistoria, devolve para análise, altera valor e favorecidos em análise, cancela. O dono usou "backoffice"; o rascunho usava "analista". Até a resposta de D27, o texto diz "backoffice (analista)" |
| Aprovador | Aprova o que passou da alçada, reprova ou devolve o que está aguardando alçada, cancela |
| Tesouraria (pessoa) | Solicita o pagamento de processo aprovado |
| Rotina automática de pagamento | A cada intervalo (D1), pede à tesouraria o pagamento dos processos em pagamento solicitado e aplica a resposta na hora. É a única que tira o processo dessa situação. Autor "sistema" |

A tesouraria (sistema dela) não é um ator que nos avisa: ela só responde quando a rotina pergunta (ver Sistemas externos).

Quem pode executar cada ação:

| Ação | Papel | Origem |
|---|---|---|
| Abrir processo | cliente (D10) | proposta |
| Alterar valor e favorecidos, processo aberto | cliente | decidido pelo dono |
| Alterar valor e favorecidos, processo em análise | backoffice (analista) | decidido pelo dono |
| Concordar ou discordar da alteração | cliente | decidido pelo dono |
| Pedir o pagamento à tesouraria e aplicar a resposta | rotina automática | decidido pelo dono |
| Iniciar análise, solicitar documento, receber documento, concluir análise, concluir vistoria (imobiliário) | backoffice (analista) | proposta |
| Reprovar | backoffice (analista) (em análise), aprovador (aguardando alçada) | proposta |
| Aprovar na alçada | aprovador | proposta |
| Devolver para análise | backoffice (analista) (de aprovado e de aguardando vistoria), aprovador (de aguardando alçada) | proposta |
| Solicitar pagamento | tesouraria (pessoa) | proposta |
| Cancelar | backoffice (analista), aprovador (D11, D29) | proposta |
| Consultar | todos os papéis | proposta |

## Vocabulário

| Termo | Significado |
|---|---|
| Cota | Cota de consórcio à qual o processo pertence; é cadastrada neste mesmo sistema, com número, cliente e valor de crédito, que quem mantém o cadastro pode alterar |
| Processo | Pedido de pagamento de uma cota, do tipo imobiliário ou serviço |
| Em andamento | Processo que ainda não chegou a uma situação final (pago, reprovado ou cancelado). Enquanto houver um, a cota fica indisponível para novo processo (R6) |
| Crédito disponível | Crédito da cota menos o valor dos processos pagos (R13). Processo em andamento não soma, porque a cota só tem um (R6), e é o próprio processo que está sendo aberto ou alterado |
| Favorecido | Quem recebe uma parte do valor do processo (ex.: vendedor, cartório) |
| Alçada | Valor acima do qual a aprovação de uma segunda pessoa é exigida |
| Pendência de documento | Processo parado à espera de um documento pedido durante a análise |
| Alteração proposta | Novo valor e novos favorecidos informados pelo backoffice (analista), que só valem se o cliente concordar (R25, R26) |
| Aguardando concordância | Situação em que o processo espera a resposta do cliente à alteração proposta; nada mais anda (R28) |
| Solicitar pagamento | Primeira etapa: a pessoa leva o processo aprovado a "pagamento solicitado". Nada é enviado à tesouraria nesse momento (R7) |
| Pagamento solicitado | Situação de espera: só a rotina automática tira o processo dela (R20, R21) |
| Rotina de pagamento | Processo automático que, a cada intervalo, pede à tesouraria o pagamento de cada processo em pagamento solicitado e aplica a resposta (R20) |
| Identificação da solicitação | Número único de cada solicitação de pagamento; a rotina o repete nas novas tentativas da mesma solicitação, para a tesouraria não criar duas ordens (R22) |
| Protocolo | Número que a tesouraria devolve ao confirmar o pagamento |

## Dados

| Dado | Obrigatório | Formato ou faixa | Observação |
|---|---|---|---|
| Cota | sim | cota existente | R1 |
| Tipo | sim | imobiliário ou serviço | R1 |
| Valor do processo | sim (D22) | maior que zero, até duas casas decimais, até o crédito disponível | R12, R13; alterável (R24, R25) |
| Favorecidos | sim, ao menos um | identificação (D4), parte de cada um (D3, D5) | partes somam o valor (R15); alteráveis (R24, R25) |
| Alteração proposta pelo backoffice (analista) | só ao alterar em análise | novo valor e novos favorecidos, com as mesmas regras do valor e dos favorecidos | guardada à parte; o valor vigente continua o anterior até a resposta (R25, D38); motivo da alteração: D32 |
| Resposta do cliente | sim, na aguardando concordância | concordo ou discordo | R26, R27 |
| Motivo | sim ao solicitar documento, reprovar, recusa da tesouraria e discordar da alteração; devolver, cancelar e alterar em D18, D11 e D32 | texto livre | fica no histórico |
| Observação | não | texto livre | fica no histórico |
| Protocolo de pagamento | sim, ao confirmar | texto devolvido pela tesouraria | guardado no processo |
| Identificação da solicitação de pagamento | automática | única por solicitação (D14) | repetida nas novas tentativas da rotina (R22) |
| Autor de cada ação | automático | pessoa ou sistema (rotina), com papel | R19 |

## Regras de negócio

| Regra | Descrição | Exemplo que cumpre | Exemplo que viola |
|---|---|---|---|
| R1 (alterada) | Um processo é aberto para uma cota, com tipo imobiliário ou serviço, valor e favorecidos. Quem abre: D10. | Cota 1001, serviço, R$ 30.000,00, um favorecido: processo aberto. | Pedido de abertura sem valor: recusado, nada é criado. |
| R2 (alterada) | Serviço: aberto, em análise, aprovado (ou aguardando alçada, R16), pagamento solicitado, pago. Em análise pode ir a pendente de documento (R10) e voltar, ou a aguardando concordância (R25) e voltar. Ver tabela do ciclo de vida. | Serviço de R$ 30.000,00 passa por aberto, em análise, aprovado, pagamento solicitado, pago. | Serviço ir de aberto direto para aprovado: recusado. |
| R3 (alterada) | Imobiliário: como o serviço, com aguardando vistoria entre a análise e a decisão de alçada: concluir análise leva à vistoria; concluir vistoria leva a aprovado ou a aguardando alçada. | Imobiliário de R$ 30.000,00: em análise, aguardando vistoria, aprovado. | Imobiliário concluir análise e ir direto a aprovado: recusado. |
| R4 (alterada) | Qualquer situação não final pode ser cancelada, exceto pagamento solicitado (em aguardando concordância: D29). Pago, reprovado e cancelado são finais. | Processo aprovado, cancelar: vai para cancelado. | Processo em pagamento solicitado, cancelar: recusado, continua em pagamento solicitado. |
| R5 (alterada só na redação: sem código técnico de resposta) | Ação fora da ordem é recusada e a situação não muda. | Processo aberto, iniciar análise: aceito. | Processo aberto, solicitar pagamento: recusado, continua aberto. |
| R6 (mantida; CONFIRMADA, decisão do dono em 13/11/2026) | Enquanto houver processo da cota que não chegou a uma situação final (pago, reprovado ou cancelado), a cota fica indisponível para novo processo. Motivo dado pelo dono: cada cota fica indisponível para utilização até o processo chegar a um estado final, o que coíbe fraudes. | Cota 1001 só com processos pagos, reprovados ou cancelados: abre outro. | Cota 1001 com processo em análise (ou aprovado, ou em pagamento solicitado): novo processo recusado. |
| R7 (alterada; decisão do dono em 13/11/2026) | Solicitar pagamento de processo aprovado apenas leva o processo a pagamento solicitado. Nada é enviado à tesouraria nesse momento, e a solicitação não depende de a tesouraria estar disponível. O protocolo só existe quando a tesouraria confirma (R20). | Processo aprovado, solicitar pagamento com a tesouraria fora do ar: fica em pagamento solicitado, sem protocolo. | Processo em análise, solicitar pagamento: recusado (R5). Ou: a solicitação enviar a ordem à tesouraria na hora. |
| R8 (alterada; decisão do dono em 13/11/2026) | A tesouraria indisponível, ou que não responde, não recusa mais a solicitação: o processo fica em pagamento solicitado, sem protocolo, e a rotina tenta de novo no próximo intervalo, com a mesma identificação (R22). | Tesouraria fora do ar às 10:00 e às 10:05: processo continua em pagamento solicitado; às 10:10 ela responde e a rotina aplica a resposta. | Tesouraria fora do ar e o processo voltar a aprovado, ou ser dado como pago, sem resposta dela. |
| R9 (alterada só na redação) | Toda recusa informa à pessoa uma mensagem com o motivo e, quando o problema é de dado, quais dados. O formato exato é definido na implementação. | Valor negativo: mensagem diz que o valor deve ser maior que zero e aponta o valor. | Recusa sem mensagem nenhuma. |
| R10 (nova) | Na análise, pode-se solicitar documento, informando o motivo (obrigatório): o processo vai a pendente de documento. Ao receber o documento, volta para em análise. | Em análise, solicitar documento com motivo "certidão vencida": pendente de documento; receber documento: volta para em análise. | Solicitar documento sem motivo: recusado, continua em análise. |
| R11 (nova) | Na análise ou aguardando alçada, pode-se reprovar, informando o motivo (obrigatório): o processo vai a reprovado (final). | Aguardando alçada, reprovar com motivo "favorecido divergente": reprovado. | Reprovar com motivo em branco: recusado, situação não muda. |
| R12 (alterada) | O valor do processo, na abertura e em toda alteração, é maior que zero e tem no máximo duas casas decimais. | R$ 12.500,50. | R$ 0,00; R$ 100,005. |
| R13 (alterada; decorre da decisão do dono sobre R6) | O valor do processo, na abertura e em toda alteração, não pode passar do crédito disponível da cota: crédito da cota menos o valor dos processos pagos. Não há outro processo em andamento a somar (R6). | Cota de R$ 250.000,00 com R$ 100.000,00 pagos: processo de R$ 150.000,00 é aceito; alterar um processo aberto dessa cota para R$ 140.000,00 também. | Mesma cota: processo de R$ 150.000,01 é recusado, nada é criado; alterar para R$ 150.000,01 é recusado e o valor anterior fica. |
| R14 (alterada; decorre de R6 e R13, não de resposta direta do dono, que pode contestar) | Processo reprovado ou cancelado não consome crédito e libera a cota: só o valor dos pagos conta no crédito disponível (R13), e reprovado e cancelado são situações finais (R6). Resolve D9. | Cota de R$ 250.000,00 com processo de R$ 100.000,00 cancelado: novo processo de R$ 250.000,00 é aceito. | Mesma cota: novo processo de R$ 250.000,00 recusado "por falta de crédito" ou "por processo em andamento" como se o cancelado ainda contasse. |
| R15 (alterada) | O valor pode ser dividido entre um ou mais favorecidos; na abertura e em toda alteração, as partes somam exatamente o valor do processo. | Valor R$ 10.000,00: vendedor R$ 9.500,00 e cartório R$ 500,00. | Valor R$ 10.000,00: vendedor R$ 9.500,00 e cartório R$ 400,00 (soma R$ 9.900,00): recusado. |
| R16 (nova) | Ao concluir a análise (serviço) ou a vistoria (imobiliário), valor até a alçada leva a aprovado; valor acima da alçada leva a aguardando alçada (D1). | Alçada R$ 50.000,00 (ilustrativa); processo de R$ 50.000,00: vai a aprovado. | Processo de R$ 80.000,00 ir direto a aprovado: não pode, vai a aguardando alçada. |
| R17 (nova) | Em aguardando alçada, quem aprova não pode ser quem concluiu a análise (D16). | Ana concluiu a análise; Bruno aprova: aprovado. | Ana concluiu a análise e tenta aprovar: recusado, continua aguardando alçada. |
| R18 (alterada) | Cada ação só é aceita de pessoa com o papel da ação (quadro em Atores, D10), ou da rotina quando é o resultado da tesouraria. Pedir a rotina, ou o resultado da tesouraria, não é ação de pessoa nenhuma. | Bruno (aprovador) aprova na alçada: aceito. A cliente da cota concorda com a alteração: aceito. | Bruno (aprovador) tenta iniciar análise: recusado, situação não muda. Uma pessoa tenta confirmar o pagamento: não existe essa ação. |
| R19 (alterada) | O histórico do processo guarda, para cada ação aceita e para cada resultado aplicado pela rotina, quem fez (pessoa ou sistema), quando, e a observação ou o motivo (D24, D34). | Ana solicita documento "certidão vencida" em 10/11 às 14:02: aparece no histórico com autor, data e motivo. A rotina aplica a recusa da tesouraria "conta inválida": aparece com autor "sistema" e o motivo. | Ação aceita que não aparece no histórico. |
| R20 (alterada; decisão do dono em 13/11/2026) | A cada intervalo (D1) a rotina pega os processos em pagamento solicitado, pede o pagamento à tesouraria com a identificação daquela solicitação e aplica a resposta na hora. Tesouraria confirmou com protocolo: o processo vai a pago (final) e guarda o protocolo. Só a rotina tira o processo de pagamento solicitado, e a tesouraria só responde quando a rotina pergunta. | Pagamento solicitado, a rotina pede e a tesouraria confirma com protocolo 778899: pago, protocolo 778899. | Pessoa tirar o processo de pagamento solicitado (cancelar, devolver, confirmar). Ou: confirmação sem protocolo tornar o processo pago (D19). |
| R21 (alterada; decisão do dono em 13/11/2026) | Tesouraria recusou (com motivo): o processo volta a aprovado, com o motivo no histórico, e pode ser solicitado de novo (D13, D14). | Pagamento solicitado, a rotina pede e a tesouraria recusa "conta inválida": volta a aprovado; solicitar de novo é aceito. | A recusa deixar o processo em pagamento solicitado, ou sem o motivo no histórico. |
| R22 (alterada; decisão do dono em 13/11/2026) | Tesouraria sem resposta ou indisponível: o processo fica em pagamento solicitado e a rotina tenta no próximo intervalo com a MESMA identificação da solicitação, para a tesouraria não criar duas ordens (D1, D20). | Intervalo de 5 minutos (ilustrativo); sem resposta às 10:00, nova tentativa às 10:05 com a mesma identificação. | Nova tentativa com identificação nova, e a tesouraria abrir segunda ordem do mesmo pagamento. |
| R23 (nova) | De aprovado, aguardando alçada (e aguardando vistoria no imobiliário) pode-se devolver o processo para análise (D17, D18). | Aprovado, devolver para análise: volta a em análise. | Pagamento solicitado, devolver para análise: recusado. |
| R24 (nova; decisão do dono em 13/11/2026) | Enquanto o processo está aberto, o cliente altera valor e favorecidos livremente; R12, R13 e R15 valem na alteração. O processo continua aberto. | Cota 1001, serviço aberto de R$ 30.000,00: o cliente altera para R$ 28.000,00 com um favorecido de R$ 28.000,00: vale, continua aberto. | O cliente altera para R$ 28.000,00 com favorecidos que somam R$ 27.000,00: recusado, os dados anteriores ficam. O cliente altera um processo em análise: recusado (R25). |
| R25 (nova; decisão do dono em 13/11/2026) | Depois que a análise começa, só o backoffice (analista) altera valor e favorecidos, e só com o processo em análise (R12, R13 e R15 valem). A alteração leva o processo a aguardando concordância; o valor e os favorecidos vigentes continuam os anteriores até a resposta do cliente (D38). De aguardando vistoria em diante ninguém altera: é preciso devolver para análise (R23). Em pendente de documento: D36. | Processo em análise de R$ 30.000,00: o backoffice altera para R$ 25.000,00 com favorecidos que somam R$ 25.000,00: vai a aguardando concordância, e o valor vigente segue R$ 30.000,00. | Processo aguardando vistoria: o backoffice tenta alterar: recusado, é preciso devolver para análise. |
| R26 (nova; decisão do dono em 13/11/2026) | O cliente concorda com a alteração proposta: a alteração vale (novo valor e novos favorecidos) e o processo volta para em análise. | Aguardando concordância, o cliente concorda: o valor passa a R$ 25.000,00 e o processo volta a em análise. | Concordar com o processo em análise: recusado (R5). O backoffice concordar no lugar do cliente: recusado (R18). |
| R27 (nova; decisão do dono em 13/11/2026) | O cliente discorda da alteração proposta, informando o motivo (obrigatório): a alteração é desfeita, o motivo vai ao histórico e o processo volta para em análise com os dados anteriores. | Aguardando concordância, o cliente discorda com motivo "valor errado": volta a em análise com R$ 30.000,00 e os favorecidos anteriores; o motivo está no histórico. | Discordar sem motivo: recusado, continua aguardando concordância. Discordar e a alteração continuar valendo. |
| R28 (nova; decisão do dono em 13/11/2026) | Em aguardando concordância nada mais anda até a resposta do cliente: só concordar e discordar são aceitos (cancelar: D29; desistir da alteração: D31); reprovar, devolver e as demais ações são recusadas. | Aguardando concordância, o cliente concorda: aceito. | Aguardando concordância, aprovar, reprovar, devolver ou solicitar pagamento: recusado, continua aguardando concordância. |

R6 deixa de estar em dúvida e não é riscada. R7 e R8 do caso de uso confirmado (pagar registra a ordem na hora; tesouraria indisponível recusa o pagamento) passam a valer como R7, R8, R20, R21 e R22 acima; os testes que dependem delas serão achados por essas regras. Nenhuma outra regra do caso de uso atual deixa de valer nesta proposta.

## Ciclo de vida

Situações: ABERTO, EM_ANALISE, PENDENTE_DOCUMENTO, AGUARDANDO_VISTORIA (só imobiliário), AGUARDANDO_ALCADA, AGUARDANDO_CONCORDANCIA (nova), APROVADO, PAGAMENTO_SOLICITADO, PAGO (final), REPROVADO (final), CANCELADO (final).

Célula "recusada" é a ação fora da ordem (R5, R28). Célula "dúvida Dn" depende da resposta. As ações "rotina: tesouraria confirmou" e "rotina: tesouraria recusou" NÃO são pedidos de pessoa: são os resultados que a rotina aplica ao processo em pagamento solicitado quando a tesouraria responde (R20, R21). Por isso só fazem algo em PAGAMENTO_SOLICITADO, e nas demais situações a célula diz "não se aplica". Forma escolhida: duas ações da rotina na própria tabela de cada tipo, para ver, na mesma linha, tudo o que pode acontecer com o processo. Tesouraria que não responde, ou está indisponível, não tem ação própria porque nada muda: o processo fica em PAGAMENTO_SOLICITADO e a rotina tenta de novo (R8, R22). "Alterar (cliente)" e "alterar (backoffice)" são as alterações de valor e favorecidos (R24, R25).

### Serviço

| Situação \ Ação | iniciar análise | solicitar documento | receber documento | reprovar | concluir análise | aprovar na alçada | devolver para análise | solicitar pagamento | cancelar | alterar (cliente) | alterar (backoffice) | concordar | discordar | rotina: tesouraria confirmou | rotina: tesouraria recusou |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ABERTO | vai para EM_ANALISE | recusada | recusada | dúvida D12 | recusada | recusada | recusada | recusada | vai para CANCELADO | continua ABERTO, com os novos dados | recusada | recusada | recusada | não se aplica | não se aplica |
| EM_ANALISE | recusada | vai para PENDENTE_DOCUMENTO | recusada | vai para REPROVADO | vai para APROVADO (até a alçada) ou AGUARDANDO_ALCADA (acima) | recusada | recusada | recusada | vai para CANCELADO | recusada | vai para AGUARDANDO_CONCORDANCIA | recusada | recusada | não se aplica | não se aplica |
| PENDENTE_DOCUMENTO | recusada | recusada | vai para EM_ANALISE | dúvida D12 | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | dúvida D36 | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_ALCADA | recusada | recusada | recusada | vai para REPROVADO | recusada | vai para APROVADO (outra pessoa, R17) | vai para EM_ANALISE | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_CONCORDANCIA | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | dúvida D29 | recusada | recusada | vai para EM_ANALISE, com a alteração valendo | vai para EM_ANALISE, com os dados anteriores | não se aplica | não se aplica |
| APROVADO | recusada | recusada | recusada | dúvida D12 | recusada | recusada | vai para EM_ANALISE | vai para PAGAMENTO_SOLICITADO | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| PAGAMENTO_SOLICITADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para PAGO, com o protocolo | vai para APROVADO, com o motivo no histórico |
| PAGO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| REPROVADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| CANCELADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |

### Imobiliário

Igual ao serviço, com a situação AGUARDANDO_VISTORIA e a ação "concluir vistoria". Concluir análise leva à vistoria, e é a vistoria que decide entre aprovado e aguardando alçada. Repete-se a tabela inteira, sem remissão.

| Situação \ Ação | iniciar análise | solicitar documento | receber documento | reprovar | concluir análise | concluir vistoria | aprovar na alçada | devolver para análise | solicitar pagamento | cancelar | alterar (cliente) | alterar (backoffice) | concordar | discordar | rotina: tesouraria confirmou | rotina: tesouraria recusou |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ABERTO | vai para EM_ANALISE | recusada | recusada | dúvida D12 | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | continua ABERTO, com os novos dados | recusada | recusada | recusada | não se aplica | não se aplica |
| EM_ANALISE | recusada | vai para PENDENTE_DOCUMENTO | recusada | vai para REPROVADO | vai para AGUARDANDO_VISTORIA | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | vai para AGUARDANDO_CONCORDANCIA | recusada | recusada | não se aplica | não se aplica |
| PENDENTE_DOCUMENTO | recusada | recusada | vai para EM_ANALISE | dúvida D12 | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | dúvida D36 | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_VISTORIA | recusada | recusada | recusada | dúvida D12 | recusada | vai para APROVADO (até a alçada) ou AGUARDANDO_ALCADA (acima) | recusada | dúvida D17 (proposta: vai para EM_ANALISE) | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_ALCADA | recusada | recusada | recusada | vai para REPROVADO | recusada | recusada | vai para APROVADO (outra pessoa, R17) | dúvida D17 (proposta: vai para EM_ANALISE) | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_CONCORDANCIA | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | dúvida D29 | recusada | recusada | vai para EM_ANALISE, com a alteração valendo | vai para EM_ANALISE, com os dados anteriores | não se aplica | não se aplica |
| APROVADO | recusada | recusada | recusada | dúvida D12 | recusada | recusada | recusada | vai para EM_ANALISE | vai para PAGAMENTO_SOLICITADO | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| PAGAMENTO_SOLICITADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para PAGO, com o protocolo | vai para APROVADO, com o motivo no histórico |
| PAGO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| REPROVADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| CANCELADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |

## Sistemas externos

| Sistema | O que pedimos | O que devolve | Se não responder ou recusar |
|---|---|---|---|
| Tesouraria, chamada pela rotina de pagamento (nunca ao solicitar) | O pagamento do processo em pagamento solicitado: processo, identificação da solicitação, valor e partes de cada favorecido | Na hora: confirmação com protocolo, ou recusa com motivo | Confirmou: R20 (pago, com protocolo). Recusou: R21 (volta a aprovado, motivo no histórico). Não respondeu ou indisponível: R8 e R22 (continua em pagamento solicitado e a rotina tenta de novo, com a mesma identificação). Resposta incoerente (confirmação sem protocolo, recusa sem motivo, ordem já feita em tentativa anterior sem resposta nossa): D19 |

A tesouraria não nos avisa por conta própria: só responde quando a rotina pergunta.

## Recusas

| Situação | O que a pessoa é informada | O que acontece com o dado |
|---|---|---|
| Ação fora da ordem (R5), inclusive ação repetida cujo efeito já ocorreu (ex.: iniciar análise duas vezes, concordar duas vezes) | Que a ação não vale na situação atual | Nada muda |
| Cancelar em pagamento solicitado (R4) | Que o pagamento já foi solicitado e não pode ser cancelado | Continua em pagamento solicitado |
| Qualquer ação de pessoa, exceto consultar, em pagamento solicitado (R20, D28) | Que o processo aguarda a rotina de pagamento | Nada muda |
| Qualquer ação em aguardando concordância, exceto concordar, discordar e (D29) cancelar (R28) | Que o processo aguarda a concordância do cliente | Continua aguardando concordância |
| Pessoa sem o papel da ação (R18), inclusive alterar valor e favorecidos fora do papel ou da situação (R24, R25) e outro que não o cliente concordar ou discordar | Que o papel dela não permite a ação, ou que a alteração não vale na situação atual (ex.: aguardando vistoria: devolver para análise antes) | Nada muda |
| Aprovar na alçada quem concluiu a análise (R17) | Que a aprovação deve ser de outra pessoa | Continua aguardando alçada |
| Motivo ausente em solicitar documento, reprovar e discordar da alteração (R10, R11, R27); devolver, cancelar e alterar pelo backoffice em D18, D11 e D32 | Que o motivo é obrigatório | Nada muda |
| Abrir ou alterar com valor zero, negativo ou com mais de duas casas (R12) | Que o valor é inválido | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir ou alterar com valor acima do crédito disponível (R13) | Que o valor passa do crédito disponível e quanto há disponível | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir ou alterar com favorecidos que não somam o valor, ou sem favorecido (R15) | Quanto falta ou sobra | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir para cota inexistente, tipo inválido ou cota com processo em andamento (R1, R6) | O motivo | Processo não é criado |
| Resposta incoerente da tesouraria à rotina (D19) | Nada à pessoa; fica no histórico (D24) | Continua em pagamento solicitado e a rotina tenta de novo (proposta, D19) |
| Dois pedidos ao mesmo tempo sobre o mesmo processo (ex.: aprovar e reprovar, duas solicitações de pagamento, cliente concordar e discordar, alteração do cliente e do backoffice) | Quem chega primeiro vale; o outro é tratado como ação fora da ordem (D25) | Uma única mudança de situação |
| Duas execuções da rotina ao mesmo tempo sobre o mesmo processo em pagamento solicitado | Nada (não há pessoa) | Um único pedido efetivo à tesouraria e uma única mudança de situação, e a outra execução nada faz (D25) |
| Dois pedidos de abertura ao mesmo tempo para a mesma cota (R6, R13) | Um abre; o outro é recusado | Exatamente um processo criado, e o crédito nunca passa do disponível |
| Mesmo pedido repetido (duplo clique, nova tentativa) | Ação que já ocorreu: recusada como fora da ordem | Nada muda. Repetir o pedido à tesouraria é da rotina e usa a mesma identificação (R22) |

## Consultas

| O que a pessoa precisa ver | Filtros | Canal |
|---|---|---|
| Lista de processos: identificação, cota, número da cota, tipo, situação, valor vigente, protocolo de pagamento | tipo, situação, paginação (como hoje; filtros novos em D25) | o mesmo de hoje |
| Detalhe do processo: favorecidos e partes, alteração proposta e valor vigente enquanto aguarda concordância (quem consulta: D37), motivo da última pendência, reprovação, discordância ou recusa da tesouraria, histórico completo (quem, quando, o quê, observação, tentativas da rotina em D24) | pelo processo | o mesmo de hoje |
| Crédito disponível da cota | pela cota | o mesmo de hoje (D25) |

## Fora do escopo

- Estorno depois de pago (proposta, D8).
- Anexar o arquivo do documento recebido: receber documento só registra o recebimento (D26).
- A tesouraria nos avisar por conta própria, depois, do resultado de um pagamento: ficou fora; só a rotina pergunta e a tesouraria só responde quando perguntada.
- Contrato técnico de entradas e saídas: acrescentado depois, na implementação.

## Dúvidas

Nenhuma vira regra, teste ou código enquanto estiver sem resposta.

### Abertas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D1 | Qual o valor da alçada (processo de valor igual ao limite exige segunda pessoa?) e de quanto em quanto tempo a rotina de pagamento roda (esse é o prazo em que um pagamento solicitado espera pela tesouraria)? | O dono define os dois. Proposta de leitura: "até a alçada, inclusive" dispensa segunda pessoa. Nos exemplos, R$ 50.000,00 e 5 minutos são só ilustrativos. | aguardando |
| D3 | O favorecido é informado por valor ou por percentual? | Por valor, que soma o valor do processo (R15). | aguardando |
| D4 | Que dados identificam um favorecido (nome, CPF ou CNPJ, papel como vendedor ou cartório, dados de recebimento)? | Nome, CPF ou CNPJ e papel obrigatórios; dados de recebimento ficam para a tesouraria, a confirmar. | aguardando |
| D5 | Há limite de favorecidos por processo? Cada parte precisa ser maior que zero? O mesmo favorecido pode aparecer duas vezes? | Ao menos um, sem limite máximo, cada parte maior que zero com duas casas, mesmo favorecido não repete. | aguardando |
| D8 | Há estorno depois de pago? | Fora do escopo desta entrega. | aguardando |
| D10 | Que papel executa cada ação? Quem abre o processo (o dono não disse; com o cliente alterando em aberto, é provável que seja o cliente)? Quem solicita o pagamento? Uma pessoa pode ter mais de um papel? | O quadro de ação por papel em Atores: o que está "decidido pelo dono" vale; o resto é proposta. O cliente abre o processo (o backoffice, a confirmar). Uma pessoa pode ter mais de um papel, mas a regra da segunda pessoa (R17) vale por pessoa. | aguardando |
| D11 | Quem pode cancelar e o motivo é obrigatório? | Backoffice (analista) e aprovador; motivo obrigatório. (Em aguardando concordância: D29.) | aguardando |
| D12 | Reprovar também vale em aberto, pendente de documento, aguardando vistoria e aprovado? (Em pagamento solicitado e em aguardando concordância é recusada, R28.) | Não: só em análise e aguardando alçada. Nas outras situações, recusada (cancelar continua valendo). | aguardando |
| D13 | A recusa da tesouraria volta para aprovado: o processo pode ser solicitado de novo sem limite de tentativas? | Sem limite; cada recusa fica no histórico com o motivo. | aguardando |
| D14 | Cada nova solicitação depois de uma recusa da tesouraria tem identificação nova? (Dentro da mesma solicitação, a rotina repete a identificação, R22.) | Sim: identificação nova por solicitação; só as novas tentativas da mesma solicitação mantêm a dela. | aguardando |
| D16 | "Quem concluiu a análise" é a pessoa da última conclusão (o processo pode voltar e ser concluído de novo)? No imobiliário, quem concluiu a vistoria também fica impedido de aprovar? | Vale a última conclusão de análise; quem concluiu a vistoria pode aprovar. | aguardando |
| D17 | O sistema atual já devolve para análise a partir de aguardando vistoria e de aprovado, mas o caso de uso confirmado não registra isso. Esse comportamento é confirmado como regra? E o processo aguardando alçada volta para a análise ou para a vistoria? | Confirmar como regra. Tudo volta para em análise, e a vistoria se repete depois. | aguardando |
| D18 | Devolver para análise exige motivo? O motivo tem tamanho mínimo ou máximo? | Motivo obrigatório na devolução; sem limite de tamanho definido. | aguardando |
| D19 | Com a chamada síncrona, o que ainda pode dar errado na resposta da tesouraria à rotina: confirmação sem protocolo; recusa sem motivo; ou, numa nova tentativa da mesma solicitação depois de uma tentativa sem resposta nossa, a tesouraria dizer que já fez a ordem, ou confirmar com protocolo diferente do que informou antes? O que acontece com o processo e com o protocolo? | A resposta incoerente não é aplicada: o processo continua em pagamento solicitado, nada é guardado, a rotina tenta de novo no próximo intervalo e o fato fica no histórico. Se a tesouraria disser que já fez a ordem e informar o protocolo, vale como confirmação. | aguardando |
| D20 | A rotina tem limite de tentativas para uma solicitação sem resposta? Existe prazo ou número de tentativas depois do qual alguém é avisado, e quem? A rotina só olha processos em pagamento solicitado? | Só pagamento solicitado; sem limite de tentativas e sem aviso até o dono definir; sugestão: depois de um prazo que o dono escolher, avisar a tesouraria (pessoa) de que o processo segue sem resposta. Cada tentativa no histórico (D24). | aguardando |
| D21 | Se o crédito da cota for reduzido depois de aberto o processo, a ponto de o valor do processo não caber mais, o que acontece? O crédito é conferido de novo em algum momento além da abertura e de cada alteração (R13), por exemplo ao aprovar ou ao pedir à tesouraria? Pode-se reduzir o crédito de uma cota abaixo do que já está comprometido em processos? | Conferido na abertura e em cada alteração de valor; redução posterior do crédito não afeta processo já aberto. Reduzir o crédito abaixo do comprometido não pode: a alteração da cota é recusada. | aguardando |
| D22 | O valor e os favorecidos são obrigatórios na abertura? E os processos que já existem sem valor: pelas regras novas, só o cliente (em aberto) e o backoffice (em análise) alteram, mas há processo existente em aguardando vistoria, aguardando alçada, aprovado ou pagamento solicitado, de onde ninguém altera (e o pagamento solicitado nem volta para análise, R23), e a rotina não teria valor para pedir à tesouraria. Como esses processos recebem valor e favorecidos? | Obrigatórios na abertura. Existentes: aberto, o cliente informa; em análise ou pendente de documento, o backoffice (analista) informa com a concordância do cliente; aguardando vistoria, aguardando alçada e aprovado, devolver para análise (R23) e então o backoffice informa; pagamento solicitado sem valor: a rotina não o envia à tesouraria e o dono decide o caminho (a regra nova não cobre). Os finalizados ficam como estão. | aguardando |
| D23 | Há motivo ou limite para cancelar processo aprovado que já teve a solicitação de pagamento recusada pela tesouraria? | Sem regra especial: aprovado cancela normalmente (R4). | aguardando |
| D24 | O histórico registra a rotina como autora (autor "sistema") e as tentativas dela sem resposta? Registra também o cliente (alterações e respostas) e os pedidos recusados? | Sim para a rotina (cada tentativa e cada resultado aplicado) e para o cliente; recusas de pedido não entram. | aguardando |
| D25 | Pedidos simultâneos (de pessoas, ou de duas execuções da rotina sobre o mesmo processo): quem chega primeiro vale e o outro é recusado como ação fora da ordem (a outra execução nada faz)? Que filtros novos a lista precisa (cota, situação aguardando alçada, aguardando concordância, valor) e a lista de hoje deve ter regra para página, tamanho, tipo e situação inválidos, que o caso de uso atual deixou como pergunta? | Sim para a ordem de chegada. Filtros novos: cota e situação. As perguntas abertas de hoje são repassadas ao dono sem proposta nova. | aguardando |
| D26 | Receber documento só registra o recebimento, ou o documento é anexado ao processo? Quem recebe? | Só registra o recebimento, com observação; anexar fica fora do escopo. | aguardando |
| D27 | "Backoffice" (como o dono disse) é o mesmo papel que "analista" (como o rascunho dizia), ou um papel à parte? | É o mesmo papel. Proposta: manter, depois da resposta, só o nome que o dono preferir. Até lá, o texto diz "backoffice (analista)". | aguardando |
| D28 | Em pagamento solicitado, a pessoa consegue desistir (voltar a aprovado) antes de a rotina conseguir falar com a tesouraria? | Não: só a rotina tira o processo dessa situação, como o dono definiu (R4 e R23 já recusam cancelar e devolver). | aguardando |
| D29 | Enquanto aguarda a concordância, o processo pode ser cancelado, e por quem (backoffice, aprovador, cliente)? | Sim, pelo backoffice (analista) e pelo aprovador, com motivo (D11); o cliente, a decidir. Cancelar descarta a alteração proposta. | aguardando |
| D30 | A espera pela concordância tem prazo? E se o cliente nunca responde? | Sem prazo até o dono definir; nesse tempo a cota segue indisponível (R6). Sugestão: depois de um prazo, o backoffice (analista) é avisado. | aguardando |
| D31 | O backoffice (analista) pode desistir da alteração antes de o cliente responder? | Não: nada anda até a resposta (R28). | aguardando |
| D32 | A alteração feita pelo backoffice (analista) exige motivo? | Sim, motivo obrigatório, que o cliente vê ao decidir. | aguardando |
| D33 | Depois de o cliente discordar, o backoffice (analista) pode propor outra alteração? | Sim, sem limite; cada proposta e cada resposta no histórico. | aguardando |
| D34 | A alteração do cliente em aberto entra no histórico? | Sim, com autor, data e os valores anterior e novo. | aguardando |
| D35 | Se a alteração muda o valor de modo que o processo cruze a alçada, o que muda? | Nada: a alçada é decidida ao concluir a análise ou a vistoria (R16), depois de qualquer alteração (que só é possível antes disso). | aguardando |
| D36 | O backoffice (analista) pode alterar valor e favorecidos em pendente de documento? (O dono disse "só em análise".) | Não: recusada, só em análise (R25). | aguardando |
| D37 | Quem é "o cliente" que altera, concorda ou discorda: o cliente da cota, só ele? Ele consulta a alteração proposta e por qual canal? | Só o cliente da cota altera em aberto, concorda e discorda, e consulta a proposta e o histórico. | aguardando |
| D38 | Enquanto aguarda a concordância, o valor vigente (consulta e conta do crédito, R13) continua o anterior? | Sim: a alteração só vale com a concordância (R26). | aguardando |

### Respondidas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D2 | A regra "uma cota tem no máximo um processo em andamento" (R6) continua valendo, agora que há valor e crédito disponível? | Continua (é regra confirmada e o dono não pediu mudança). Nesse caso o crédito disponível na prática é crédito menos pagos. | RESPONDIDA em 13/11/2026: continua. "É como se cada cota ficasse indisponível para utilização até que o processo chegue a um estado final. Isso é uma forma de coibir fraudes." R6 confirmada; R13 e R14 ajustadas. |
| D6 | O favorecido pode ser alterado depois de aberto o processo, até quando, e por quem? | Pode, até o fim da análise (aberto, em análise, pendente de documento); depois disso não. Cada alteração vai ao histórico. | RESPONDIDA em 13/11/2026: pode. Aberto: o cliente altera livremente. Depois que a análise começa: só o backoffice, e só em análise, com a concordância do cliente; de aguardando vistoria em diante, ninguém (é preciso devolver para análise). R24 a R28. Detalhes em aberto: D27 a D38. |
| D7 | O valor do processo pode mudar depois de aberto? | Não. Para outro valor, cancela e abre outro processo. | RESPONDIDA em 13/11/2026: pode, na mesma regra de D6 (valor e favorecidos juntos); R12, R13 e R15 valem na alteração. |
| D9 | Processo reprovado ou cancelado libera o crédito da cota? | Sim (R14). | RESPONDIDA em 13/11/2026, POR DERIVAÇÃO e não por resposta direta do dono: decorre de R6 e R13 depois da decisão sobre D2. Só o processo pago consome crédito, e reprovado e cancelado são situações finais que liberam a cota. O dono pode contestar. |
| D15 | Com a tesouraria indisponível ao solicitar pagamento, o que acontece? | Manter R8: solicitação recusada, processo continua aprovado, usuário avisado. Alternativa: ficar em pagamento solicitado e a rotina (R22) enviar depois. | RESPONDIDA em 13/11/2026: criar um estado intermediário (pagamento solicitado) que só sai mediante evento temporal, e só a rotina agendada o tira dele; solicitar nada envia à tesouraria; a tesouraria só responde quando a rotina pergunta. R7, R8, R20, R21 e R22. |
