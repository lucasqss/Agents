# Caso de uso: pagamento de processo

Situação: CONFIRMADO em 13/11/2026 por dono do produto

## Para quem mantém os testes

Na data da confirmação (13/11/2026), o sistema ainda implementa o caso de uso anterior. As regras novas e alteradas abaixo serão implementadas em fatias, e os testes de aceitação serão refeitos a cada fatia. Contrato, modelo de teste, rastreabilidade e sessão exploratória do caso de uso anterior não foram copiados para este documento: a skill de testes os refaz a partir das regras confirmadas.

**Regras alteradas** (existiam no caso de uso anterior; os testes que dependem delas mudam):

- R1: a abertura passa a exigir valor e favorecidos, e só o cliente da cota abre o processo.
- R2: o fluxo do serviço ganha pendente de documento, aguardando alçada, aguardando concordância, pagamento solicitado e reprovado; a última etapa antes de pago passa a ser pagamento solicitado.
- R3: o mesmo para o imobiliário; concluir a vistoria (e não a análise) decide entre aprovado e aguardando alçada.
- R4: reprovado também é final; cancelar passa a ser só do backoffice e do aprovador, com motivo obrigatório; cancelar em pagamento solicitado é recusado; em aguardando concordância cancelar vale e descarta a alteração.
- R7: a ação passa a se chamar solicitar pagamento e só leva o processo a pagamento solicitado; nada vai à tesouraria nesse momento, e o protocolo só existe quando a rotina recebe a confirmação (R20). No caso de uso anterior, pagar registrava a ordem na hora.
- R8: a tesouraria indisponível deixa de recusar o pagamento: o processo fica em pagamento solicitado e a rotina tenta de novo. No caso de uso anterior, o pagamento era recusado e o processo seguia aprovado.

**Regras que mudaram só a redação** (os testes não mudam): R5 e R9 (sem código técnico de resposta e sem formato de mensagem). R6 continua a mesma regra (no máximo um processo em andamento por cota); a redação agora diz o motivo e quais situações contam como em andamento.

**Regras novas** (o caso de uso anterior só tinha R1 a R9):

- R10: solicitar documento (com motivo) leva a pendente de documento; receber o documento volta para em análise.
- R11: reprovar, com motivo, só em análise ou aguardando alçada; leva a reprovado (final).
- R12: valor maior que zero, com até duas casas.
- R13: valor até o crédito disponível da cota (crédito menos o valor dos pagos).
- R14: reprovado e cancelado não consomem crédito e liberam a cota.
- R15: favorecidos: partes em valor, maiores que zero, somando exatamente o valor; o mesmo favorecido não se repete.
- R16: ao concluir a análise (serviço) ou a vistoria (imobiliário), até R$ 50.000,00, inclusive, vai a aprovado; acima vai a aguardando alçada.
- R17: em aguardando alçada, quem aprova não pode ser quem concluiu a análise.
- R18: cada ação só é aceita de pessoa com o papel da ação (ou da rotina, quando é o resultado da tesouraria).
- R19: histórico de cada ação aceita e de cada resultado da rotina (autor, data, observação ou motivo). O sistema já guardava um histórico que o caso de uso anterior não registrava.
- R20: a rotina, a cada 5 minutos, pede à tesouraria o pagamento dos processos em pagamento solicitado e aplica a resposta; confirmação com protocolo leva a pago.
- R21: recusa da tesouraria volta o processo a aprovado, com o motivo no histórico; pode ser solicitado de novo sem limite.
- R22: tesouraria sem resposta: continua em pagamento solicitado; nova tentativa a cada 5 minutos com a mesma identificação, sem limite de tentativas e sem aviso a ninguém.
- R23: devolver para análise, com motivo, de aprovado, aguardando vistoria e aguardando alçada. O sistema já fazia isso e o caso de uso anterior não registrava.
- R24: em aberto, o cliente altera valor e favorecidos livremente.
- R25: em análise, só o backoffice altera valor e favorecidos, com motivo; leva a aguardando concordância, e o valor vigente continua o anterior até a resposta do cliente.
- R26: o cliente concorda: a alteração vale e o processo volta para em análise.
- R27: o cliente discorda, com motivo: a alteração é desfeita e o processo volta para em análise.
- R28: em aguardando concordância só concordar, discordar e cancelar são aceitos; sem prazo.
- R29: cada favorecido é identificado por nome, CPF ou CNPJ e papel, todos obrigatórios.
- R30: implantação: cada processo existente recebe o valor do crédito da cota e um favorecido com o nome do cliente da cota; as regras novas valem dali em diante.
- R31: resposta incoerente da tesouraria à rotina não é aplicada.

**Regras removidas:** nenhuma.

**Defeitos e perguntas ainda abertos da sessão exploratória do caso de uso anterior** (para não se perderem na substituição):

- Consultas (lista), pergunta ao negócio: página zero ou negativa responde com itens; tamanho zero ou negativo é tratado como 1; tamanho muito grande responde sem erro; tipo ou situação desconhecidos no filtro respondem lista vazia, sem erro. Continua pergunta aberta (D39).
- R1, pergunta ao negócio: campo desconhecido no pedido de abertura é ignorado e o processo é aberto. Continua pergunta aberta (D39).
- Defeitos já corrigidos, que não são pendência e servem de teste de regressão: R6 (pedidos de abertura simultâneos para a mesma cota criam exatamente um processo; os demais são recusados) e R9 (toda recusa, inclusive formato inválido, identificação que não é número, formato de pedido não aceito e pedido malformado, informa uma mensagem).
- Confirmado pela sessão, sem pendência: processo ou cota com identificação zero ou negativa são tratados como inexistentes, com mensagem; ação repetida é recusada (R5).

## Objetivo

Permitir que a pessoa da área acompanhe o pagamento de um processo de uma cota, do início ao fim: análise (com pendência de documento, reprovação e devolução), aprovação por uma segunda pessoa quando o valor passa da alçada, e pagamento em duas etapas: a pessoa solicita e uma rotina automática pede o pagamento à tesouraria. O valor do processo é limitado ao crédito disponível da cota e pode ser dividido entre favorecidos; o cliente os altera enquanto o processo está aberto e, depois, só o backoffice altera, com a concordância do cliente. Cada cota fica indisponível para novo processo até o processo chegar a uma situação final. Tudo fica registrado: quem fez, quando e por quê.

## Atores

| Ator | O que faz nesta capacidade |
|---|---|
| Cliente (o cliente da cota, só ele) | Abre o processo; altera valor e favorecidos enquanto o processo está aberto; concorda ou discorda (com motivo) da alteração feita pelo backoffice; consulta a alteração proposta e o histórico. Não cancela |
| Backoffice | Analisa, pede e recebe documento, reprova, conclui a análise e a vistoria, devolve para análise, altera valor e favorecidos em análise, cancela. Não abre processo nesta entrega |
| Aprovador | Aprova o que passou da alçada, reprova ou devolve o que está aguardando alçada, cancela |
| Tesouraria (pessoa) | Solicita o pagamento de processo aprovado |
| Rotina automática de pagamento | A cada 5 minutos (configurável), pede à tesouraria o pagamento dos processos em pagamento solicitado e aplica a resposta na hora. É a única que tira o processo dessa situação. Autor "sistema" |

A tesouraria (sistema dela) não é um ator que nos avisa: ela só responde quando a rotina pergunta (ver Sistemas externos).

Quem pode executar cada ação:

| Ação | Papel | Origem |
|---|---|---|
| Abrir processo | cliente | dono, 13/11/2026 (D10) |
| Alterar valor e favorecidos, processo aberto | cliente | dono, 13/11/2026 |
| Alterar valor e favorecidos, processo em análise | backoffice | dono, 13/11/2026 |
| Concordar ou discordar da alteração | cliente | dono, 13/11/2026 (D37) |
| Pedir o pagamento à tesouraria e aplicar a resposta | rotina automática | dono, 13/11/2026 |
| Iniciar análise, solicitar documento, receber documento, concluir análise, concluir vistoria (imobiliário) | backoffice | dono, 13/11/2026 (D10, D26, D27) |
| Reprovar | backoffice (em análise), aprovador (aguardando alçada) | dono, 13/11/2026 (D10, D12) |
| Aprovar na alçada | aprovador | dono, 13/11/2026 (D10) |
| Devolver para análise | backoffice (de aprovado e de aguardando vistoria), aprovador (de aguardando alçada) | dono, 13/11/2026 (D10, D17) |
| Solicitar pagamento | tesouraria (pessoa) | dono, 13/11/2026 (D10) |
| Cancelar | backoffice, aprovador (inclusive em aguardando concordância); o cliente não cancela | dono, 13/11/2026 (D11, D29) |
| Consultar | todos os papéis | dono, 13/11/2026 (D10) |

Uma pessoa pode ter mais de um papel? Sim, mas a regra da segunda pessoa (R17) vale por pessoa (dono, 13/11/2026, D10). Há ações que exigem pessoas diferentes? Sim: aprovar na alçada exige pessoa diferente da que concluiu a análise (R17).

## Vocabulário

| Termo | Significado |
|---|---|
| Cota | Cota de consórcio à qual o processo pertence; é cadastrada neste mesmo sistema, com número, cliente e valor de crédito, que quem mantém o cadastro pode alterar |
| Processo | Pedido de pagamento de uma cota, do tipo imobiliário ou serviço |
| Em andamento | Processo que ainda não chegou a uma situação final (pago, reprovado ou cancelado). Enquanto houver um, a cota fica indisponível para novo processo (R6) |
| Crédito disponível | Crédito da cota menos o valor dos processos pagos (R13). Processo em andamento não soma, porque a cota só tem um (R6), e é o próprio processo que está sendo aberto ou alterado |
| Favorecido | Quem recebe uma parte do valor do processo (ex.: vendedor, cartório) |
| Alçada | Valor acima do qual a aprovação de uma segunda pessoa é exigida: R$ 50.000,00 (configurável; o valor igual à alçada não exige segunda pessoa) |
| Pendência de documento | Processo parado à espera de um documento pedido durante a análise |
| Alteração proposta | Novo valor e novos favorecidos informados pelo backoffice, que só valem se o cliente concordar (R25, R26) |
| Aguardando concordância | Situação em que o processo espera a resposta do cliente à alteração proposta; nada mais anda (R28). Não tem prazo |
| Solicitar pagamento | Primeira etapa: a pessoa leva o processo aprovado a "pagamento solicitado". Nada é enviado à tesouraria nesse momento (R7) |
| Pagamento solicitado | Situação de espera: só a rotina automática tira o processo dela (R20, R21) |
| Rotina de pagamento | Processo automático que, a cada 5 minutos (configurável), pede à tesouraria o pagamento de cada processo em pagamento solicitado e aplica a resposta (R20) |
| Identificação da solicitação | Número único de cada solicitação de pagamento; a rotina o repete nas novas tentativas da mesma solicitação, para a tesouraria não criar duas ordens (R22) |
| Protocolo | Número que a tesouraria devolve ao confirmar o pagamento |

## Dados

| Dado | Obrigatório | Formato ou faixa | Observação |
|---|---|---|---|
| Cota | sim | cota existente | R1 |
| Tipo | sim | imobiliário ou serviço | R1 |
| Valor do processo | sim | maior que zero, até duas casas decimais, até o crédito disponível | R12, R13; alterável (R24, R25) |
| Favorecidos | sim, ao menos um, sem limite máximo | nome, CPF ou CNPJ e papel (vendedor, cartório etc.), todos obrigatórios; parte de cada um em valor, maior que zero, com duas casas | partes somam o valor (R15); o mesmo favorecido não repete; alteráveis (R24, R25); R29 |
| Alteração proposta pelo backoffice | só ao alterar em análise | novo valor e novos favorecidos, com as mesmas regras do valor e dos favorecidos | guardada à parte; o valor vigente continua o anterior até a resposta (R25) |
| Motivo da alteração | sim, ao alterar em análise | texto livre | o cliente o vê ao decidir (R25) |
| Resposta do cliente | sim, na aguardando concordância | concordo ou discordo | R26, R27 |
| Motivo | sim ao solicitar documento, reprovar, cancelar, devolver para análise, alterar em análise e discordar da alteração | texto livre, sem limite de tamanho definido | fica no histórico |
| Observação | não | texto livre | fica no histórico |
| Protocolo de pagamento | sim, ao confirmar | texto devolvido pela tesouraria | guardado no processo |
| Identificação da solicitação de pagamento | automática | nova a cada solicitação | repetida nas novas tentativas da rotina (R22) |
| Autor de cada ação | automático | pessoa ou sistema (rotina), com papel | R19 |

## Regras de negócio

| Regra | Descrição | Exemplo que cumpre | Exemplo que viola | Origem |
|---|---|---|---|---|
| R1 (alterada) | Um processo é aberto pelo cliente da cota, para uma cota, com tipo imobiliário ou serviço, valor e favorecidos, todos obrigatórios. O backoffice não abre processo nesta entrega. | Cota 1001, serviço, R$ 30.000,00, um favorecido: processo aberto pelo cliente. | Pedido de abertura sem valor: recusado, nada é criado. | dono, 13/11/2026 (D10, D22) |
| R2 (alterada) | Serviço: aberto, em análise, aprovado (ou aguardando alçada, R16), pagamento solicitado, pago. Em análise pode ir a pendente de documento (R10) e voltar, ou a aguardando concordância (R25) e voltar. Ver o quadro do ciclo de vida. | Serviço de R$ 30.000,00 passa por aberto, em análise, aprovado, pagamento solicitado, pago. | Serviço ir de aberto direto para aprovado: recusado. | dono, 13/11/2026 |
| R3 (alterada) | Imobiliário: como o serviço, com aguardando vistoria entre a análise e a decisão de alçada: concluir análise leva à vistoria; concluir vistoria leva a aprovado ou a aguardando alçada. | Imobiliário de R$ 30.000,00: em análise, aguardando vistoria, aprovado. | Imobiliário concluir análise e ir direto a aprovado: recusado. | dono, 13/11/2026 |
| R4 (alterada) | Qualquer situação não final pode ser cancelada, pelo backoffice ou pelo aprovador, informando o motivo (obrigatório), exceto pagamento solicitado. Em aguardando concordância, cancelar vale e descarta a alteração proposta. Pago, reprovado e cancelado são finais. Processo aprovado que teve o pagamento recusado pela tesouraria cancela normalmente. | Processo aprovado, o backoffice cancela com motivo "desistência da cota": vai para cancelado. Aguardando concordância, o aprovador cancela com motivo: cancelado, e a alteração proposta é descartada. | Processo em pagamento solicitado, cancelar: recusado, continua em pagamento solicitado. Cancelar sem motivo: recusado. O cliente cancelar: recusado. | dono, 13/11/2026 (D11, D23, D29) |
| R5 (alterada só na redação: sem código técnico de resposta) | Ação fora da ordem é recusada e a situação não muda. | Processo aberto, iniciar análise: aceito. | Processo aberto, solicitar pagamento: recusado, continua aberto. | dono, 13/11/2026 |
| R6 | Enquanto houver processo da cota que não chegou a uma situação final (pago, reprovado ou cancelado), a cota fica indisponível para novo processo. Motivo dado pelo dono: cada cota fica indisponível para utilização até o processo chegar a um estado final, o que coíbe fraudes. | Cota 1001 só com processos pagos, reprovados ou cancelados: abre outro. | Cota 1001 com processo em análise (ou aprovado, ou em pagamento solicitado): novo processo recusado. | dono, 13/11/2026 (D2) |
| R7 (alterada) | Solicitar pagamento de processo aprovado apenas leva o processo a pagamento solicitado. Nada é enviado à tesouraria nesse momento, e a solicitação não depende de a tesouraria estar disponível. O protocolo só existe quando a tesouraria confirma (R20). | Processo aprovado, solicitar pagamento com a tesouraria fora do ar: fica em pagamento solicitado, sem protocolo. | Processo em análise, solicitar pagamento: recusado (R5). Ou: a solicitação enviar a ordem à tesouraria na hora. | dono, 13/11/2026 (D15) |
| R8 (alterada) | A tesouraria indisponível, ou que não responde, não recusa a solicitação: o processo fica em pagamento solicitado, sem protocolo, e a rotina tenta de novo no próximo intervalo, com a mesma identificação (R22). | Tesouraria fora do ar às 10:00 e às 10:05: processo continua em pagamento solicitado; às 10:10 ela responde e a rotina aplica a resposta. | Tesouraria fora do ar e o processo voltar a aprovado, ou ser dado como pago, sem resposta dela. | dono, 13/11/2026 (D15) |
| R9 (alterada só na redação) | Toda recusa informa à pessoa uma mensagem com o motivo e, quando o problema é de dado, quais dados. O formato exato é definido na implementação. | Valor negativo: mensagem diz que o valor deve ser maior que zero e aponta o valor. | Recusa sem mensagem nenhuma. | dono, 13/11/2026 |
| R10 (nova) | Na análise, pode-se solicitar documento, informando o motivo (obrigatório): o processo vai a pendente de documento. Ao receber o documento, volta para em análise. Receber documento só registra o recebimento, com observação; o arquivo não é anexado. | Em análise, solicitar documento com motivo "certidão vencida": pendente de documento; receber documento: volta para em análise. | Solicitar documento sem motivo: recusado, continua em análise. | dono, 13/11/2026 (D26) |
| R11 (nova) | Na análise ou aguardando alçada, e só nelas, pode-se reprovar, informando o motivo (obrigatório): o processo vai a reprovado (final). | Aguardando alçada, reprovar com motivo "favorecido divergente": reprovado. | Reprovar com motivo em branco: recusado, situação não muda. Reprovar um processo aprovado: recusado. | dono, 13/11/2026 (D12) |
| R12 (nova) | O valor do processo, na abertura e em toda alteração, é maior que zero e tem no máximo duas casas decimais. | R$ 12.500,50. | R$ 0,00; R$ 100,005. | dono, 13/11/2026 (D22) |
| R13 (nova) | O valor do processo, na abertura e em toda alteração, não pode passar do crédito disponível da cota: crédito da cota menos o valor dos processos pagos. Não há outro processo em andamento a somar (R6). | Cota de R$ 250.000,00 com R$ 100.000,00 pagos: processo de R$ 150.000,00 é aceito; alterar um processo aberto dessa cota para R$ 140.000,00 também. | Mesma cota: processo de R$ 150.000,01 é recusado, nada é criado; alterar para R$ 150.000,01 é recusado e o valor anterior fica. | dono, 13/11/2026 (D2) |
| R14 (nova) | Processo reprovado ou cancelado não consome crédito e libera a cota: só o valor dos pagos conta no crédito disponível (R13), e reprovado e cancelado são situações finais (R6). | Cota de R$ 250.000,00 com processo de R$ 100.000,00 cancelado: novo processo de R$ 250.000,00 é aceito. | Mesma cota: novo processo de R$ 250.000,00 recusado "por falta de crédito" ou "por processo em andamento" como se o cancelado ainda contasse. | decorrência de R6 e R13 (D9); o dono pode contestar |
| R15 (nova) | O valor pode ser dividido entre um ou mais favorecidos, sem limite máximo; na abertura e em toda alteração, as partes são informadas em valor, cada uma maior que zero e com no máximo duas casas, e somam exatamente o valor do processo. O mesmo favorecido não aparece duas vezes no mesmo processo. | Valor R$ 10.000,00: vendedor R$ 9.500,00 e cartório R$ 500,00. | Valor R$ 10.000,00: vendedor R$ 9.500,00 e cartório R$ 400,00 (soma R$ 9.900,00): recusado. Mesmo CPF duas vezes: recusado. Parte de R$ 0,00: recusada. | dono, 13/11/2026 (D3, D5) |
| R16 (nova) | Ao concluir a análise (serviço) ou a vistoria (imobiliário), valor até a alçada, inclusive, leva a aprovado; valor acima da alçada leva a aguardando alçada. A alçada é de R$ 50.000,00 e pode mudar sem mudar a regra. A alçada é decidida nesse momento, depois de qualquer alteração de valor (que só é possível antes dele). | Processo de exatamente R$ 50.000,00: vai a aprovado. Processo de R$ 50.000,01: vai a aguardando alçada. | Processo de R$ 80.000,00 ir direto a aprovado: não pode, vai a aguardando alçada. Processo de R$ 50.000,00 ir a aguardando alçada. | dono, 13/11/2026 (D1, D35) |
| R17 (nova) | Em aguardando alçada, quem aprova não pode ser quem concluiu a análise (a pessoa da última conclusão de análise). No imobiliário, quem concluiu a vistoria pode aprovar. | Ana concluiu a análise; Bruno aprova: aprovado. | Ana concluiu a análise e tenta aprovar: recusado, continua aguardando alçada. | dono, 13/11/2026 (D16) |
| R18 (nova) | Cada ação só é aceita de pessoa com o papel da ação (quadro em Atores), ou da rotina quando é o resultado da tesouraria. Pedir a rotina, ou o resultado da tesouraria, não é ação de pessoa nenhuma. Só o cliente da cota abre, altera, concorda e discorda. | Bruno (aprovador) aprova na alçada: aceito. O cliente da cota concorda com a alteração: aceito. | Bruno (aprovador) tenta iniciar análise: recusado, situação não muda. Uma pessoa tenta confirmar o pagamento: não existe essa ação. Outro cliente concorda com a alteração: recusado. | dono, 13/11/2026 (D10, D37) |
| R19 (nova) | O histórico do processo guarda, para cada ação aceita (inclusive as do cliente, com o valor anterior e o novo nas alterações) e para cada tentativa e resultado aplicado pela rotina, quem fez (pessoa ou sistema), quando, e a observação ou o motivo. Pedidos recusados não entram. | Ana solicita documento "certidão vencida" em 10/11 às 14:02: aparece no histórico com autor, data e motivo. A rotina aplica a recusa da tesouraria "conta inválida": aparece com autor "sistema" e o motivo. | Ação aceita que não aparece no histórico. | dono, 13/11/2026 (D24, D34) |
| R20 (nova) | A cada 5 minutos (configurável) a rotina pega os processos em pagamento solicitado, pede o pagamento à tesouraria com a identificação daquela solicitação e aplica a resposta na hora. Tesouraria confirmou com protocolo: o processo vai a pago (final) e guarda o protocolo. Só a rotina tira o processo de pagamento solicitado (a pessoa não desiste dele), e a tesouraria só responde quando a rotina pergunta. A rotina só olha processos em pagamento solicitado. | Pagamento solicitado, a rotina pede e a tesouraria confirma com protocolo 778899: pago, protocolo 778899. | Pessoa tirar o processo de pagamento solicitado (cancelar, devolver, confirmar). Ou: confirmação sem protocolo tornar o processo pago (R31). | dono, 13/11/2026 (D1, D15, D20, D28) |
| R21 (nova) | Tesouraria recusou (com motivo): o processo volta a aprovado, com o motivo no histórico, e pode ser solicitado de novo, sem limite de vezes. Cada nova solicitação tem identificação nova. | Pagamento solicitado, a rotina pede e a tesouraria recusa "conta inválida": volta a aprovado; solicitar de novo é aceito, com identificação nova. | A recusa deixar o processo em pagamento solicitado, ou sem o motivo no histórico. Nova solicitação reaproveitar a identificação da anterior. | dono, 13/11/2026 (D13, D14) |
| R22 (nova) | Tesouraria sem resposta ou indisponível: o processo fica em pagamento solicitado e a rotina tenta no próximo intervalo (5 minutos) com a MESMA identificação da solicitação, para a tesouraria não criar duas ordens. A rotina não tem limite de tentativas e ninguém é avisado, nesta entrega; cada tentativa vai ao histórico (R19). | Sem resposta às 10:00, nova tentativa às 10:05 com a mesma identificação. | Nova tentativa com identificação nova, e a tesouraria abrir segunda ordem do mesmo pagamento. A rotina desistir depois de certo número de tentativas. | dono, 13/11/2026 (D1, D20) |
| R23 (nova) | De aprovado, aguardando alçada e (no imobiliário) aguardando vistoria pode-se devolver o processo para análise, informando o motivo (obrigatório, sem limite de tamanho definido). Tudo volta para em análise, e a vistoria se repete depois. | Aprovado, devolver para análise com motivo "documento divergente": volta a em análise. | Pagamento solicitado, devolver para análise: recusado. Devolver sem motivo: recusado. | dono, 13/11/2026 (D17, D18) |
| R24 (nova) | Enquanto o processo está aberto, o cliente da cota altera valor e favorecidos livremente; R12, R13 e R15 valem na alteração. O processo continua aberto. | Cota 1001, serviço aberto de R$ 30.000,00: o cliente altera para R$ 28.000,00 com um favorecido de R$ 28.000,00: vale, continua aberto. | O cliente altera para R$ 28.000,00 com favorecidos que somam R$ 27.000,00: recusado, os dados anteriores ficam. O cliente altera um processo em análise: recusado (R25). | dono, 13/11/2026 (D37) |
| R25 (nova) | Depois que a análise começa, só o backoffice altera valor e favorecidos, e só com o processo em análise (R12, R13 e R15 valem), informando o motivo (obrigatório), que o cliente vê ao decidir. A alteração leva o processo a aguardando concordância; o valor e os favorecidos vigentes (consulta e conta do crédito) continuam os anteriores até a resposta do cliente. Em pendente de documento, aguardando vistoria e em diante ninguém altera: é preciso devolver para análise (R23). | Processo em análise de R$ 30.000,00: o backoffice altera para R$ 25.000,00 com favorecidos que somam R$ 25.000,00 e motivo "valor errado": vai a aguardando concordância, e o valor vigente segue R$ 30.000,00. | Processo aguardando vistoria: o backoffice tenta alterar: recusado, é preciso devolver para análise. Processo pendente de documento: recusado. Alterar sem motivo: recusado. | dono, 13/11/2026 (D32, D36, D38) |
| R26 (nova) | O cliente da cota concorda com a alteração proposta: a alteração vale (novo valor e novos favorecidos) e o processo volta para em análise. | Aguardando concordância, o cliente concorda: o valor passa a R$ 25.000,00 e o processo volta a em análise. | Concordar com o processo em análise: recusado (R5). O backoffice concordar no lugar do cliente: recusado (R18). | dono, 13/11/2026 |
| R27 (nova) | O cliente da cota discorda da alteração proposta, informando o motivo (obrigatório): a alteração é desfeita, o motivo vai ao histórico e o processo volta para em análise com os dados anteriores. O backoffice pode propor outra alteração depois, sem limite; cada proposta e cada resposta vão ao histórico. | Aguardando concordância, o cliente discorda com motivo "valor errado": volta a em análise com R$ 30.000,00 e os favorecidos anteriores; o motivo está no histórico. | Discordar sem motivo: recusado, continua aguardando concordância. Discordar e a alteração continuar valendo. | dono, 13/11/2026 (D33) |
| R28 (nova) | Em aguardando concordância nada mais anda até a resposta do cliente: só concordar, discordar e cancelar (pelo backoffice ou pelo aprovador, R4) são aceitos; reprovar, devolver, o backoffice desistir da alteração e as demais ações são recusados. A espera não tem prazo: a cota fica indisponível (R6) enquanto o cliente não responde, e o cancelamento é a saída. Ninguém é avisado depois de um prazo, nesta entrega. | Aguardando concordância, o cliente concorda: aceito. O backoffice cancela com motivo: cancelado. | Aguardando concordância, aprovar, reprovar, devolver ou solicitar pagamento: recusado, continua aguardando concordância. O backoffice desistir da alteração: recusado. O cliente cancelar: recusado. | dono, 13/11/2026 (D29, D30, D31) |
| R29 (nova) | Cada favorecido é identificado por nome, CPF ou CNPJ e papel (como vendedor ou cartório), todos obrigatórios. Dados de recebimento não são informados nem guardados neste caso de uso nesta entrega. | Favorecido "Cartório Central", CNPJ informado, papel cartório: aceito. | Favorecido sem CPF ou CNPJ: recusado, nada é criado. | dono, 13/11/2026 (D4) |
| R30 (nova) | Implantação: cada processo que já existe recebe o valor do crédito da cota e um favorecido com o nome do cliente da cota. As regras novas valem dali em diante. O sistema de exemplo não tem dados reais. | Processo existente de cota com crédito de R$ 250.000,00: passa a ter valor R$ 250.000,00 e um favorecido com o nome do cliente da cota. | Processo existente continuar sem valor depois da implantação. | dono, 13/11/2026 (D22) |
| R31 (nova) | Resposta da tesouraria à rotina que não faz sentido (confirmação sem protocolo; recusa sem motivo; protocolo diferente do que ela informou antes para a mesma solicitação) não é aplicada: o processo continua em pagamento solicitado, nada é guardado, o fato vai ao histórico e a rotina tenta de novo no próximo intervalo. Se a tesouraria diz que já fez a ordem daquela solicitação e informa o protocolo, vale como confirmação (R20). | Confirmação sem protocolo às 10:00: continua em pagamento solicitado, fato no histórico, nova tentativa às 10:05. A tesouraria diz que a ordem já foi feita, protocolo 778899: pago. | Confirmação sem protocolo tornar o processo pago. | dono, 13/11/2026 (D19) |

R6 deixou de estar em dúvida e não é riscada. R7 e R8 do caso de uso anterior (pagar registra a ordem na hora; tesouraria indisponível recusa o pagamento) passam a valer como R7, R8, R20, R21 e R22 acima. Nenhuma outra regra do caso de uso anterior deixa de valer.

## Ciclo de vida

Situações: ABERTO, EM_ANALISE, PENDENTE_DOCUMENTO, AGUARDANDO_VISTORIA (só imobiliário), AGUARDANDO_ALCADA, AGUARDANDO_CONCORDANCIA, APROVADO, PAGAMENTO_SOLICITADO, PAGO (final), REPROVADO (final), CANCELADO (final).

Cada célula é "vai para X" (com condição quando houver), "recusada" ou "não se aplica". "Recusada" é a ação fora da ordem (R5, R28) ou o que as regras proíbem. As ações "rotina: tesouraria confirmou" e "rotina: tesouraria recusou" NÃO são pedidos de pessoa: são os resultados que a rotina aplica ao processo em pagamento solicitado quando a tesouraria responde (R20, R21). Por isso só fazem algo em PAGAMENTO_SOLICITADO, e nas demais situações a célula diz "não se aplica". Tesouraria que não responde, ou está indisponível, não tem ação própria porque nada muda: o processo fica em PAGAMENTO_SOLICITADO e a rotina tenta de novo (R8, R22). "Alterar (cliente)" e "alterar (backoffice)" são as alterações de valor e favorecidos (R24, R25). "Cancelar" é do backoffice e do aprovador (R4). Valor até a alçada, inclusive, vai a APROVADO; acima, a AGUARDANDO_ALCADA (R16).

### Serviço

| Situação \ Ação | iniciar análise | solicitar documento | receber documento | reprovar | concluir análise | aprovar na alçada | devolver para análise | solicitar pagamento | cancelar | alterar (cliente) | alterar (backoffice) | concordar | discordar | rotina: tesouraria confirmou | rotina: tesouraria recusou |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ABERTO | vai para EM_ANALISE | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | continua ABERTO, com os novos dados | recusada | recusada | recusada | não se aplica | não se aplica |
| EM_ANALISE | recusada | vai para PENDENTE_DOCUMENTO | recusada | vai para REPROVADO | vai para APROVADO (até a alçada, inclusive) ou AGUARDANDO_ALCADA (acima) | recusada | recusada | recusada | vai para CANCELADO | recusada | vai para AGUARDANDO_CONCORDANCIA | recusada | recusada | não se aplica | não se aplica |
| PENDENTE_DOCUMENTO | recusada | recusada | vai para EM_ANALISE | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_ALCADA | recusada | recusada | recusada | vai para REPROVADO | recusada | vai para APROVADO (outra pessoa, R17) | vai para EM_ANALISE | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_CONCORDANCIA | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO (backoffice ou aprovador; descarta a alteração proposta) | recusada | recusada | vai para EM_ANALISE, com a alteração valendo | vai para EM_ANALISE, com os dados anteriores | não se aplica | não se aplica |
| APROVADO | recusada | recusada | recusada | recusada | recusada | recusada | vai para EM_ANALISE | vai para PAGAMENTO_SOLICITADO | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| PAGAMENTO_SOLICITADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para PAGO, com o protocolo | vai para APROVADO, com o motivo no histórico |
| PAGO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| REPROVADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| CANCELADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |

### Imobiliário

Igual ao serviço, com a situação AGUARDANDO_VISTORIA e a ação "concluir vistoria". Concluir análise leva à vistoria, e é a vistoria que decide entre aprovado e aguardando alçada. Repete-se o quadro inteiro, sem remissão.

| Situação \ Ação | iniciar análise | solicitar documento | receber documento | reprovar | concluir análise | concluir vistoria | aprovar na alçada | devolver para análise | solicitar pagamento | cancelar | alterar (cliente) | alterar (backoffice) | concordar | discordar | rotina: tesouraria confirmou | rotina: tesouraria recusou |
|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|---|
| ABERTO | vai para EM_ANALISE | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | continua ABERTO, com os novos dados | recusada | recusada | recusada | não se aplica | não se aplica |
| EM_ANALISE | recusada | vai para PENDENTE_DOCUMENTO | recusada | vai para REPROVADO | vai para AGUARDANDO_VISTORIA | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | vai para AGUARDANDO_CONCORDANCIA | recusada | recusada | não se aplica | não se aplica |
| PENDENTE_DOCUMENTO | recusada | recusada | vai para EM_ANALISE | recusada | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_VISTORIA | recusada | recusada | recusada | recusada | recusada | vai para APROVADO (até a alçada, inclusive) ou AGUARDANDO_ALCADA (acima) | recusada | vai para EM_ANALISE | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_ALCADA | recusada | recusada | recusada | vai para REPROVADO | recusada | recusada | vai para APROVADO (outra pessoa, R17) | vai para EM_ANALISE | recusada | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| AGUARDANDO_CONCORDANCIA | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para CANCELADO (backoffice ou aprovador; descarta a alteração proposta) | recusada | recusada | vai para EM_ANALISE, com a alteração valendo | vai para EM_ANALISE, com os dados anteriores | não se aplica | não se aplica |
| APROVADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para EM_ANALISE | vai para PAGAMENTO_SOLICITADO | vai para CANCELADO | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| PAGAMENTO_SOLICITADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | vai para PAGO, com o protocolo | vai para APROVADO, com o motivo no histórico |
| PAGO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| REPROVADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |
| CANCELADO | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | recusada | não se aplica | não se aplica |

## Sistemas externos

| Sistema | O que pedimos | O que devolve | Se não responder ou recusar |
|---|---|---|---|
| Tesouraria, chamada pela rotina de pagamento (nunca ao solicitar) | O pagamento do processo em pagamento solicitado: processo, identificação da solicitação, valor e partes de cada favorecido | Na hora: confirmação com protocolo, ou recusa com motivo | Confirmou: R20 (pago, com protocolo). Recusou: R21 (volta a aprovado, motivo no histórico). Não respondeu ou indisponível: R8 e R22 (continua em pagamento solicitado e a rotina tenta de novo, com a mesma identificação, sem limite de tentativas e sem aviso). Resposta incoerente (confirmação sem protocolo, recusa sem motivo, protocolo diferente do informado antes): R31 |

A tesouraria não nos avisa por conta própria: só responde quando a rotina pergunta.

## Recusas

| Situação | O que a pessoa é informada | O que acontece com o dado |
|---|---|---|
| Ação fora da ordem (R5), inclusive ação repetida cujo efeito já ocorreu (ex.: iniciar análise duas vezes, concordar duas vezes) | Que a ação não vale na situação atual | Nada muda |
| Cancelar em pagamento solicitado (R4) | Que o pagamento já foi solicitado e não pode ser cancelado | Continua em pagamento solicitado |
| Qualquer ação de pessoa, exceto consultar, em pagamento solicitado (R20) | Que o processo aguarda a rotina de pagamento | Nada muda |
| Qualquer ação em aguardando concordância, exceto concordar, discordar e cancelar (R28) | Que o processo aguarda a concordância do cliente | Continua aguardando concordância |
| Pessoa sem o papel da ação (R18), inclusive alterar valor e favorecidos fora do papel ou da situação (R24, R25), cliente cancelar e outro que não o cliente da cota concordar ou discordar | Que o papel dela não permite a ação, ou que a alteração não vale na situação atual (ex.: aguardando vistoria: devolver para análise antes) | Nada muda |
| Aprovar na alçada quem concluiu a análise (R17) | Que a aprovação deve ser de outra pessoa | Continua aguardando alçada |
| Motivo ausente em solicitar documento, reprovar, cancelar, devolver para análise, alterar em análise e discordar da alteração (R4, R10, R11, R23, R25, R27) | Que o motivo é obrigatório | Nada muda |
| Abrir ou alterar com valor zero, negativo ou com mais de duas casas (R12) | Que o valor é inválido | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir ou alterar com valor acima do crédito disponível (R13) | Que o valor passa do crédito disponível e quanto há disponível | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir ou alterar com favorecidos que não somam o valor, sem favorecido, com parte zero, repetidos ou sem nome, CPF ou CNPJ e papel (R15, R29) | Quanto falta ou sobra, ou qual dado do favorecido está faltando | Processo não é criado; na alteração, os dados anteriores ficam |
| Abrir para cota inexistente, tipo inválido, sem valor ou sem favorecido, ou cota com processo em andamento (R1, R6) | O motivo | Processo não é criado |
| Resposta incoerente da tesouraria à rotina (R31) | Nada à pessoa; fica no histórico (R19) | Continua em pagamento solicitado e a rotina tenta de novo |
| Dois pedidos ao mesmo tempo sobre o mesmo processo (ex.: aprovar e reprovar, duas solicitações de pagamento, cliente concordar e discordar, alteração do cliente e do backoffice) | Quem chega primeiro vale; o outro é tratado como ação fora da ordem | Uma única mudança de situação |
| Duas execuções da rotina ao mesmo tempo sobre o mesmo processo em pagamento solicitado | Nada (não há pessoa) | Um único pedido efetivo à tesouraria e uma única mudança de situação, e a outra execução nada faz |
| Dois pedidos de abertura ao mesmo tempo para a mesma cota (R6, R13) | Um abre; o outro é recusado | Exatamente um processo criado, e o crédito nunca passa do disponível |
| Mesmo pedido repetido (duplo clique, nova tentativa) | Ação que já ocorreu: recusada como fora da ordem | Nada muda. Repetir o pedido à tesouraria é da rotina e usa a mesma identificação (R22) |

A regra de quem chega primeiro vale também para duas execuções da rotina (dono, 13/11/2026, D25).

## Consultas

| O que a pessoa precisa ver | Filtros | Canal |
|---|---|---|
| Lista de processos: identificação, cota, número da cota, tipo, situação, valor vigente, protocolo de pagamento | tipo, situação (inclusive as novas), cota e paginação. Comportamento com página, tamanho, tipo ou situação inválidos: pergunta aberta D39 | o mesmo de hoje |
| Detalhe do processo: favorecidos e partes, alteração proposta e valor vigente enquanto aguarda concordância (o cliente da cota consulta a proposta e o histórico), motivo da última pendência, reprovação, discordância ou recusa da tesouraria, histórico completo (quem, quando, o quê, observação, tentativas da rotina) | pelo processo | o mesmo de hoje |
| Crédito disponível da cota | pela cota | o mesmo de hoje |

## Fora do escopo

- Estorno depois de pago (dono, 13/11/2026, D8).
- Anexar o arquivo do documento recebido: receber documento só registra o recebimento (D26).
- Avisar alguém depois de um prazo: tanto quando a rotina segue sem resposta da tesouraria (D20) quanto quando o cliente segue sem responder à alteração (D30). Nesta entrega não há limite de tentativas nem prazo, e ninguém é avisado.
- Dados de recebimento do favorecido (D4): não são informados nem guardados neste caso de uso.
- O backoffice abrir processo e o cliente cancelar processo, inclusive enquanto aguarda a concordância (D10, D29): nesta entrega só o cliente abre, e o cliente não cancela.
- A tesouraria nos avisar por conta própria, depois, do resultado de um pagamento: só a rotina pergunta e a tesouraria só responde quando perguntada.
- Contrato técnico de entradas e saídas: acrescentado depois, na implementação.

## Decisões de solução citadas pelo dono

Fora das regras: não são requisito de negócio e não geram teste. Servem a quem for implementar, que as lê antes de escolher a solução.

| Quando | O que o dono disse, nas palavras dele | Regra relacionada |
|---|---|---|
| 13/11/2026 | "Criar estado intermediário e só sair dele mediante evento temporal. Pensando em sagas transacionais, podemos seguir pensando no fairy tale saga" (sobre o pagamento) | R7, R8, R20, R21, R22 |
| 13/11/2026 | "endpoint simples do cliente concordar com as mudanças" (sobre a concordância) | R26 (e R27, a discordância) |

## Dúvidas

Nenhuma vira regra, teste ou código enquanto estiver sem resposta. Dúvida respondida não é apagada nem renumerada: passa para "Respondidas". Em 13/11/2026 o dono leu as 33 dúvidas que estavam abertas e aceitou a proposta escrita em cada uma; os pontos em que a proposta não trazia valor ou deixava uma parte "a decidir" estão indicados na resposta.

### Abertas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D21 | Se o crédito da cota for reduzido depois de aberto o processo, a ponto de o valor do processo não caber mais, o que acontece? O crédito é conferido de novo em algum momento além da abertura e de cada alteração (R13), por exemplo ao aprovar ou ao pedir à tesouraria? Pode-se reduzir o crédito de uma cota abaixo do que já está comprometido em processos? | Conferido na abertura e em cada alteração de valor; redução posterior do crédito não afeta processo já aberto. Reduzir o crédito abaixo do comprometido não pode: a alteração da cota é recusada. | aguardando: o dono aceitou a proposta em 13/11/2026, mas ela não diz o que é "comprometido" (só os pagos, ou também o processo em andamento) e a regra recai sobre o cadastro da cota, fora desta capacidade. Nenhuma regra depende desta dúvida |
| D39 | A lista de processos deve ter regra para página zero ou negativa, tamanho de página zero, negativo ou muito grande, tipo ou situação desconhecidos no filtro, e para campo desconhecido no pedido de abertura? (Hoje: página zero ou negativa responde com itens; tamanho zero ou negativo vale 1; tamanho muito grande responde sem erro; tipo ou situação desconhecidos respondem lista vazia; campo desconhecido é ignorado.) | Sem proposta. Vem de D25: o dono aceitou repassar estas perguntas sem proposta nova. | aguardando |

### Respondidas

| Dúvida | Pergunta exata | Proposta | Resposta |
|---|---|---|---|
| D1 | Qual o valor da alçada (processo de valor igual ao limite exige segunda pessoa?) e de quanto em quanto tempo a rotina de pagamento roda (esse é o prazo em que um pagamento solicitado espera pela tesouraria)? | O dono define os dois. Proposta de leitura: "até a alçada, inclusive" dispensa segunda pessoa. Nos exemplos, R$ 50.000,00 e 5 minutos são só ilustrativos. | RESPONDIDA em 13/11/2026: aceita a proposta. Alçada de R$ 50.000,00; valor igual à alçada dispensa a segunda pessoa (só acima dela vai a aguardando alçada). A rotina de pagamento roda a cada 5 minutos. Os dois valores são configuráveis (podem mudar sem mudar a regra). R16, R20, R22. |
| D2 | A regra "uma cota tem no máximo um processo em andamento" (R6) continua valendo, agora que há valor e crédito disponível? | Continua (é regra confirmada e o dono não pediu mudança). Nesse caso o crédito disponível na prática é crédito menos pagos. | RESPONDIDA em 13/11/2026: continua. "É como se cada cota ficasse indisponível para utilização até que o processo chegue a um estado final. Isso é uma forma de coibir fraudes." R6 confirmada; R13 e R14 ajustadas. |
| D3 | O favorecido é informado por valor ou por percentual? | Por valor, que soma o valor do processo (R15). | RESPONDIDA em 13/11/2026: aceita a proposta. Por valor, que soma o valor do processo. R15. |
| D4 | Que dados identificam um favorecido (nome, CPF ou CNPJ, papel como vendedor ou cartório, dados de recebimento)? | Nome, CPF ou CNPJ e papel obrigatórios; dados de recebimento ficam para a tesouraria, a confirmar. | RESPONDIDA em 13/11/2026: aceita a proposta. Nome, CPF ou CNPJ e papel, obrigatórios. A parte "dados de recebimento, a confirmar" ficou sem decisão e, por isso, NÃO é permitida nesta entrega: esses dados não são informados nem guardados neste caso de uso. R29. |
| D5 | Há limite de favorecidos por processo? Cada parte precisa ser maior que zero? O mesmo favorecido pode aparecer duas vezes? | Ao menos um, sem limite máximo, cada parte maior que zero com duas casas, mesmo favorecido não repete. | RESPONDIDA em 13/11/2026: aceita a proposta. Ao menos um, sem limite máximo, cada parte maior que zero com duas casas, mesmo favorecido não repete. R15. |
| D6 | O favorecido pode ser alterado depois de aberto o processo, até quando, e por quem? | Pode, até o fim da análise (aberto, em análise, pendente de documento); depois disso não. Cada alteração vai ao histórico. | RESPONDIDA em 13/11/2026: pode. Aberto: o cliente altera livremente. Depois que a análise começa: só o backoffice, e só em análise, com a concordância do cliente; de aguardando vistoria em diante, ninguém (é preciso devolver para análise). R24 a R28. Detalhes: D27 a D38, respondidas em 13/11/2026. |
| D7 | O valor do processo pode mudar depois de aberto? | Não. Para outro valor, cancela e abre outro processo. | RESPONDIDA em 13/11/2026: pode, na mesma regra de D6 (valor e favorecidos juntos); R12, R13 e R15 valem na alteração. |
| D8 | Há estorno depois de pago? | Fora do escopo desta entrega. | RESPONDIDA em 13/11/2026: aceita a proposta. Fora do escopo desta entrega. |
| D9 | Processo reprovado ou cancelado libera o crédito da cota? | Sim (R14). | RESPONDIDA em 13/11/2026, POR DECORRÊNCIA de R6 e R13 depois da decisão sobre D2, não por resposta direta do dono: só o processo pago consome crédito, e reprovado e cancelado são situações finais que liberam a cota. O dono pode contestar. |
| D10 | Que papel executa cada ação? Quem abre o processo (o dono não disse; com o cliente alterando em aberto, é provável que seja o cliente)? Quem solicita o pagamento? Uma pessoa pode ter mais de um papel? | O quadro de ação por papel em Atores: o que está "decidido pelo dono" vale; o resto é proposta. O cliente abre o processo (o backoffice, a confirmar). Uma pessoa pode ter mais de um papel, mas a regra da segunda pessoa (R17) vale por pessoa. | RESPONDIDA em 13/11/2026: aceita a proposta. O quadro de ação por papel vale como está em Atores. O cliente abre o processo; a parte "o backoffice, a confirmar" ficou sem decisão e NÃO é permitida nesta entrega (só o cliente abre). Uma pessoa pode ter mais de um papel, mas a regra da segunda pessoa (R17) vale por pessoa. R1, R18. |
| D11 | Quem pode cancelar e o motivo é obrigatório? | Backoffice e aprovador; motivo obrigatório. (Em aguardando concordância: D29.) | RESPONDIDA em 13/11/2026: aceita a proposta. Backoffice e aprovador cancelam; motivo obrigatório. R4. |
| D12 | Reprovar também vale em aberto, pendente de documento, aguardando vistoria e aprovado? (Em pagamento solicitado e em aguardando concordância é recusada, R28.) | Não: só em análise e aguardando alçada. Nas outras situações, recusada (cancelar continua valendo). | RESPONDIDA em 13/11/2026: aceita a proposta. Reprovar só em análise e aguardando alçada; nas outras situações, recusada. R11. |
| D13 | A recusa da tesouraria volta para aprovado: o processo pode ser solicitado de novo sem limite de tentativas? | Sem limite; cada recusa fica no histórico com o motivo. | RESPONDIDA em 13/11/2026: aceita a proposta. Sem limite; cada recusa fica no histórico com o motivo. R21. |
| D14 | Cada nova solicitação depois de uma recusa da tesouraria tem identificação nova? (Dentro da mesma solicitação, a rotina repete a identificação, R22.) | Sim: identificação nova por solicitação; só as novas tentativas da mesma solicitação mantêm a dela. | RESPONDIDA em 13/11/2026: aceita a proposta. Identificação nova por solicitação; só as novas tentativas da mesma solicitação mantêm a dela. R21, R22. |
| D15 | Com a tesouraria indisponível ao solicitar pagamento, o que acontece? | Manter R8: solicitação recusada, processo continua aprovado, usuário avisado. Alternativa: ficar em pagamento solicitado e a rotina (R22) enviar depois. | RESPONDIDA em 13/11/2026: pagamento solicitado é uma situação de espera da qual só a rotina agendada tira o processo; solicitar nada envia à tesouraria; a tesouraria só responde quando a rotina pergunta. R7, R8, R20, R21 e R22. (As palavras do dono sobre a solução estão em "Decisões de solução citadas pelo dono".) |
| D16 | "Quem concluiu a análise" é a pessoa da última conclusão (o processo pode voltar e ser concluído de novo)? No imobiliário, quem concluiu a vistoria também fica impedido de aprovar? | Vale a última conclusão de análise; quem concluiu a vistoria pode aprovar. | RESPONDIDA em 13/11/2026: aceita a proposta. Vale a última conclusão de análise; quem concluiu a vistoria pode aprovar. R17. |
| D17 | O sistema atual já devolve para análise a partir de aguardando vistoria e de aprovado, mas o caso de uso confirmado não registra isso. Esse comportamento é confirmado como regra? E o processo aguardando alçada volta para a análise ou para a vistoria? | Confirmar como regra. Tudo volta para em análise, e a vistoria se repete depois. | RESPONDIDA em 13/11/2026: aceita a proposta. Confirmado como regra; tudo volta para em análise, e a vistoria se repete depois. R23. |
| D18 | Devolver para análise exige motivo? O motivo tem tamanho mínimo ou máximo? | Motivo obrigatório na devolução; sem limite de tamanho definido. | RESPONDIDA em 13/11/2026: aceita a proposta. Motivo obrigatório na devolução; sem limite de tamanho definido. R23. |
| D19 | Com a resposta da tesouraria na hora, o que ainda pode dar errado na resposta dela à rotina: confirmação sem protocolo; recusa sem motivo; ou, numa nova tentativa da mesma solicitação depois de uma tentativa sem resposta nossa, a tesouraria dizer que já fez a ordem, ou confirmar com protocolo diferente do que informou antes? O que acontece com o processo e com o protocolo? | A resposta incoerente não é aplicada: o processo continua em pagamento solicitado, nada é guardado, a rotina tenta de novo no próximo intervalo e o fato fica no histórico. Se a tesouraria disser que já fez a ordem e informar o protocolo, vale como confirmação. | RESPONDIDA em 13/11/2026: aceita a proposta. A resposta incoerente não é aplicada: continua em pagamento solicitado, nada é guardado, a rotina tenta de novo no próximo intervalo e o fato fica no histórico; se a tesouraria disser que já fez a ordem e informar o protocolo, vale como confirmação. R31. |
| D20 | A rotina tem limite de tentativas para uma solicitação sem resposta? Existe prazo ou número de tentativas depois do qual alguém é avisado, e quem? A rotina só olha processos em pagamento solicitado? | Só pagamento solicitado; sem limite de tentativas e sem aviso até o dono definir; sugestão: depois de um prazo que o dono escolher, avisar a tesouraria (pessoa) de que o processo segue sem resposta. Cada tentativa no histórico (D24). | RESPONDIDA em 13/11/2026: aceita a proposta. A rotina só olha pagamento solicitado, não tem limite de tentativas e ninguém é avisado, nesta entrega. Avisar alguém depois de um prazo fica fora do escopo. R20, R22. |
| D22 | O valor e os favorecidos são obrigatórios na abertura? E os processos que já existem sem valor: pelas regras novas, só o cliente (em aberto) e o backoffice (em análise) alteram, mas há processo existente em aguardando vistoria, aguardando alçada, aprovado ou pagamento solicitado, de onde ninguém altera (e o pagamento solicitado nem volta para análise, R23), e a rotina não teria valor para pedir à tesouraria. Como esses processos recebem valor e favorecidos? | Obrigatórios na abertura. Existentes: aberto, o cliente informa; em análise ou pendente de documento, o backoffice informa com a concordância do cliente; aguardando vistoria, aguardando alçada e aprovado, devolver para análise (R23) e então o backoffice informa; pagamento solicitado sem valor: a rotina não o envia à tesouraria e o dono decide o caminho (a regra nova não cobre). Os finalizados ficam como estão. | RESPONDIDA em 13/11/2026: aceita a proposta; o dono definiu o caminho que a proposta deixava para ele. Valor e favorecidos são obrigatórios na abertura. Processos já existentes: o sistema de exemplo não tem dados reais; na implantação, cada processo existente recebe o valor do crédito da cota e um favorecido com o nome do cliente da cota, e as regras novas valem dali em diante. R1, R30. |
| D23 | Há motivo ou limite para cancelar processo aprovado que já teve a solicitação de pagamento recusada pela tesouraria? | Sem regra especial: aprovado cancela normalmente (R4). | RESPONDIDA em 13/11/2026: aceita a proposta. Sem regra especial: aprovado cancela normalmente. R4. |
| D24 | O histórico registra a rotina como autora (autor "sistema") e as tentativas dela sem resposta? Registra também o cliente (alterações e respostas) e os pedidos recusados? | Sim para a rotina (cada tentativa e cada resultado aplicado) e para o cliente; recusas de pedido não entram. | RESPONDIDA em 13/11/2026: aceita a proposta. Sim para a rotina (cada tentativa e cada resultado aplicado) e para o cliente; recusas de pedido não entram. R19. |
| D25 | Pedidos simultâneos (de pessoas, ou de duas execuções da rotina sobre o mesmo processo): quem chega primeiro vale e o outro é recusado como ação fora da ordem (a outra execução nada faz)? Que filtros novos a lista precisa (cota, situação aguardando alçada, aguardando concordância, valor) e a lista de hoje deve ter regra para página, tamanho, tipo e situação inválidos, que o caso de uso atual deixou como pergunta? | Sim para a ordem de chegada. Filtros novos: cota e situação. As perguntas abertas de hoje são repassadas ao dono sem proposta nova. | RESPONDIDA em 13/11/2026, em parte: aceita a proposta. Quem chega primeiro vale e o outro é recusado como ação fora da ordem (a outra execução da rotina nada faz); filtros novos: cota e situação. A parte "perguntas abertas de hoje repassadas sem proposta nova" não tem decisão a registrar e passou para a dúvida D39, que continua aberta. Seção Recusas e Consultas. |
| D26 | Receber documento só registra o recebimento, ou o documento é anexado ao processo? Quem recebe? | Só registra o recebimento, com observação; anexar fica fora do escopo. | RESPONDIDA em 13/11/2026: aceita a proposta. Só registra o recebimento, com observação; anexar fica fora do escopo. R10. |
| D27 | "Backoffice" (como o dono disse) é o mesmo papel que "analista" (como o rascunho dizia), ou um papel à parte? | É o mesmo papel. Proposta: manter, depois da resposta, só o nome que o dono preferir. | RESPONDIDA em 13/11/2026: aceita a proposta. Backoffice e analista são o mesmo papel, e o nome é backoffice. |
| D28 | Em pagamento solicitado, a pessoa consegue desistir (voltar a aprovado) antes de a rotina conseguir falar com a tesouraria? | Não: só a rotina tira o processo dessa situação, como o dono definiu (R4 e R23 já recusam cancelar e devolver). | RESPONDIDA em 13/11/2026: aceita a proposta. Não: só a rotina tira o processo dessa situação. R4, R20, R23. |
| D29 | Enquanto aguarda a concordância, o processo pode ser cancelado, e por quem (backoffice, aprovador, cliente)? | Sim, pelo backoffice e pelo aprovador, com motivo (D11); o cliente, a decidir. Cancelar descarta a alteração proposta. | RESPONDIDA em 13/11/2026: aceita a proposta. Sim, pelo backoffice e pelo aprovador, com motivo; cancelar descarta a alteração proposta. A parte "o cliente, a decidir" ficou sem decisão e NÃO é permitida nesta entrega: o cliente não cancela enquanto espera a concordância. R4, R28. |
| D30 | A espera pela concordância tem prazo? E se o cliente nunca responde? | Sem prazo até o dono definir; nesse tempo a cota segue indisponível (R6). Sugestão: depois de um prazo, o backoffice é avisado. | RESPONDIDA em 13/11/2026: aceita a proposta. A espera pela concordância do cliente não tem prazo. A cota fica indisponível enquanto ele não responde; o backoffice (ou o aprovador) pode cancelar, como em D29. Avisar alguém depois de um prazo fica fora do escopo. R28. |
| D31 | O backoffice pode desistir da alteração antes de o cliente responder? | Não: nada anda até a resposta (R28). | RESPONDIDA em 13/11/2026: aceita a proposta. Não: nada anda até a resposta. R28. |
| D32 | A alteração feita pelo backoffice exige motivo? | Sim, motivo obrigatório, que o cliente vê ao decidir. | RESPONDIDA em 13/11/2026: aceita a proposta. Sim, motivo obrigatório, que o cliente vê ao decidir. R25. |
| D33 | Depois de o cliente discordar, o backoffice pode propor outra alteração? | Sim, sem limite; cada proposta e cada resposta no histórico. | RESPONDIDA em 13/11/2026: aceita a proposta. Sim, sem limite; cada proposta e cada resposta no histórico. R27. |
| D34 | A alteração do cliente em aberto entra no histórico? | Sim, com autor, data e os valores anterior e novo. | RESPONDIDA em 13/11/2026: aceita a proposta. Sim, com autor, data e os valores anterior e novo. R19. |
| D35 | Se a alteração muda o valor de modo que o processo cruze a alçada, o que muda? | Nada: a alçada é decidida ao concluir a análise ou a vistoria (R16), depois de qualquer alteração (que só é possível antes disso). | RESPONDIDA em 13/11/2026: aceita a proposta. Nada: a alçada é decidida ao concluir a análise ou a vistoria, depois de qualquer alteração. R16. |
| D36 | O backoffice pode alterar valor e favorecidos em pendente de documento? (O dono disse "só em análise".) | Não: recusada, só em análise (R25). | RESPONDIDA em 13/11/2026: aceita a proposta. Não: recusada, só em análise. R25. |
| D37 | Quem é "o cliente" que altera, concorda ou discorda: o cliente da cota, só ele? Ele consulta a alteração proposta e por qual canal? | Só o cliente da cota altera em aberto, concorda e discorda, e consulta a proposta e o histórico. | RESPONDIDA em 13/11/2026: aceita a proposta. Só o cliente da cota altera em aberto, concorda e discorda, e consulta a proposta e o histórico. R18, R24, R26, R27. |
| D38 | Enquanto aguarda a concordância, o valor vigente (consulta e conta do crédito, R13) continua o anterior? | Sim: a alteração só vale com a concordância (R26). | RESPONDIDA em 13/11/2026: aceita a proposta. Sim: a alteração só vale com a concordância. R25, R26. |
