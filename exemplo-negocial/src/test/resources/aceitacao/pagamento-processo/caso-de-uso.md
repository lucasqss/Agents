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

## Contrato usado

Seção mantida pela skill de testes. Estado: **fatia 7 de 7** (a implementação da fatia 7 ainda não existe; o contrato abaixo foi definido por quem coordena a implementação e é o que os testes usam). O que não está aqui não é conhecido pelos testes.

### Ações sobre o processo (fatia 1)

Todas `POST /processos/{id}/<acao>`, resposta `200` com `{"estado": "<SITUACAO>"}`.

| Ação | Corpo |
|---|---|
| `iniciar-analise`, `concluir-analise`, `concluir-vistoria`, `receber-documento` | opcional: `{"observacao": "texto"}` (até 500 caracteres). Sem corpo e sem Content-Type também vale |
| `solicitar-documento`, `reprovar`, `devolver-para-analise`, `cancelar` | obrigatório: `{"motivo": "texto"}`, não vazio nem só espaços, até 500 caracteres |
| `solicitar-pagamento` (fatia 5) | opcional: `{"observacao": "texto"}`, como as ações sem dado. Substitui `pagar`, que deixou de existir |

| Status | Quando |
|---|---|
| 400 | motivo ausente, vazio, só espaços ou acima de 500 caracteres; corpo ausente em ação que exige motivo; JSON malformado. Quando o problema é o motivo, `campos` traz o campo `motivo`. O processo não muda |
| 404 | processo inexistente |
| 409 | ação fora da ordem (R5); a situação não muda |
| 415 | corpo com conteúdo e tipo que não é JSON |
| 503 | deixou de existir na fatia 5: a tesouraria indisponível não recusa nada (R8) |

Formato único de erro: `{"mensagem": "...", "campos": [{"campo": "...", "mensagem": "..."}]}`.

### Consultas

| Operação | Resposta |
|---|---|
| `GET /processos/{id}/acoes` | `{"acoes": ["iniciar-analise", ...]}`: as ações aceitas na situação atual, com os nomes das ações acima |
| `GET /processos/{id}/historico` | lista em ordem cronológica; cada item tem `de`, `para`, `acao`, `quando`, `observacao`. O motivo informado aparece em `observacao` (um campo só, para motivo e observação). `acao` segue o padrão observado no sistema de hoje: nome do caminho em maiúsculas com `_` (`INICIAR_ANALISE`, `CONCLUIR_VISTORIA`, `CANCELAR`); as novas seguem o mesmo padrão (`SOLICITAR_DOCUMENTO`, `RECEBER_DOCUMENTO`, `REPROVAR`, `DEVOLVER_PARA_ANALISE`, `SOLICITAR_PAGAMENTO`). As entradas da rotina estão em "Pagamento como saga (fatia 5)" |
| `GET /processos?tipo=&estado=&pagina=&tamanho=` | `{"itens": [{"id", "cotaId", "tipo", "estado", "protocoloPagamento", ...}], "pagina", "tamanho", "total"}`; `estado` aceita também `PENDENTE_DOCUMENTO` e `REPROVADO` |

### Valor e crédito (fatia 2)

Decidido pelo time de implementação; os testes usam só isto.

| Operação | Contrato |
|---|---|
| `POST /processos` | `{"cotaId": n, "tipo": "IMOBILIARIO"\|"SERVICO", "valor": 12500.50}`; `valor` é número JSON. `201` com `{"id"}` |
| 400 | `campos` apontando `cotaId`, `tipo` ou `valor` (ausente, nulo, zero, negativo, mais de duas casas decimais, ex.: 100.005); corpo vazio ou malformado. O 400 vem antes de qualquer outra conferência (cota inexistente, cota com processo em andamento) e nada é criado |
| 404 | cota inexistente (com valor válido) |
| 409 | cota com processo em andamento (R6) **e** valor acima do crédito disponível (R13). Nada é criado e a cota continua livre. Escolha registrada como dúvida (D2-1) |
| `GET /processos` | cada item ganha `valor` (número, comparado numericamente como `BigDecimal`) |
| `GET /cotas/{id}` | `200` com `creditoDisponivel` (número) = `valorCredito` menos a soma dos valores dos processos PAGOS da cota. Em andamento, reprovado e cancelado não alteram; pago reduz |

### Favorecidos (fatia 3)

Decidido por quem coordena a implementação; os testes usam só isto. Vale para a ABERTURA. A alteração está em "Alteração de valor e favorecidos (fatia 4)".

| Operação | Contrato |
|---|---|
| `POST /processos` | `{"cotaId": n, "tipo": "...", "valor": 10000.00, "favorecidos": [{"nome": "...", "documento": "12345678901", "papel": "vendedor", "valor": 9500.00}, {"nome": "Cartório Central", "documento": "12345678000199", "papel": "cartório", "valor": 500.00}]}`. `favorecidos` é obrigatório (R1, R15). `201` com `{"id"}` |
| Campos do favorecido | `nome` e `papel`: texto não vazio nem só espaços; `nome` até 120 caracteres, `papel` até 50. `documento`: texto só com dígitos, 11 (CPF) ou 14 (CNPJ); **nesta entrega não se confere o dígito verificador** (dúvida F3-1). `valor`: número JSON maior que zero, no máximo duas casas |
| 400 de FORMATO | `campos` preenchido, com ao menos um campo cujo nome contém `favorecidos` (o caminho exato não é fixado): lista ausente, nula ou vazia; favorecido sem nome, sem documento ou sem papel (ausente, nulo ou em branco); documento com tamanho diferente de 11 ou 14 ou com caractere não dígito; parte ausente, zero, negativa ou com mais de duas casas; nome ou papel acima do tamanho |
| 400 de REGRA | `mensagem`; a lista `campos` não é conferida: soma das partes diferente do valor do processo (a menos ou a mais, inclusive por um centavo); mesmo documento duas vezes no processo |
| Precedência | todo 400 de FORMATO (inclusive o de `valor`, fatia 2) vem antes de 404 (cota inexistente) e de 409 (cota com processo em andamento, crédito insuficiente). Entre 400 de REGRA de favorecidos e 404 ou 409 o caso de uso não dá base: não testado (dúvida F3-2) |
| Recusado | nada é criado: a cota continua livre e o crédito disponível não muda |
| `GET /processos/{id}/favorecidos` | `200` com a lista `[{"nome", "documento", "papel", "valor"}]`, na ordem em que foram informados (a ordem é do contrato, dúvida F3-5); `valor` é número, comparado como `BigDecimal`. Processo inexistente: `404` com `mensagem` |
| `GET /processos` e `GET /cotas/{id}` | não mudam |

Canais (`/web`, `/mobile`, `/taa`) ficam fora dos testes de aceitação desta fatia.

### Alteração de valor e favorecidos, com concordância do cliente (fatia 4)

Decidido por quem coordena a implementação; os testes usam só isto. Papéis e identidade: ver "Identidade, papéis e autor (fatia 7)". Situação nova: `AGUARDANDO_CONCORDANCIA`.

| Operação | Corpo | Sucesso |
|---|---|---|
| `POST /processos/{id}/alterar` | `{"valor": n, "favorecidos": [...]}`, mesmo formato de favorecidos da abertura | só em ABERTO: `200 {"estado": "ABERTO"}` |
| `POST /processos/{id}/propor-alteracao` | `{"valor": n, "favorecidos": [...], "motivo": "..."}`; motivo não vazio, até 500 | só em EM_ANALISE: `200 {"estado": "AGUARDANDO_CONCORDANCIA"}` |
| `POST /processos/{id}/concordar` | opcional `{"observacao"}`, como as ações sem dado | só em AGUARDANDO_CONCORDANCIA: `200 {"estado": "EM_ANALISE"}` |
| `POST /processos/{id}/discordar` | obrigatório `{"motivo"}` | só em AGUARDANDO_CONCORDANCIA: `200 {"estado": "EM_ANALISE"}` |
| `cancelar` (já existe, com motivo) | | passa a valer em AGUARDANDO_CONCORDANCIA: CANCELADO |

| Status | Quando |
|---|---|
| 400 de FORMATO | `campos` preenchido: valor, favorecidos ou motivo ausentes ou inválidos, com as mesmas regras de formato da abertura. Vem antes de 404 e de 409 |
| 400 de REGRA | `mensagem`: soma das partes diferente do valor, documento repetido |
| 404 | processo inexistente |
| 409 | ação fora da situação (R5, R28); valor acima do crédito disponível (R13) |
| 415 | corpo com conteúdo e tipo que não é JSON |

Em toda recusa nada muda: situação, valor vigente, favorecidos vigentes, histórico e proposta pendente. A precedência entre 400 de REGRA e 404 ou 409 não é fixada (dúvida F4-1).

| Consulta | Contrato |
|---|---|
| Dados vigentes | `GET /processos` (campo `valor` do item), `GET /processos/{id}/favorecidos` e `GET /cotas/{id}` (`creditoDisponivel`): os anteriores enquanto AGUARDANDO_CONCORDANCIA; os novos depois de concordar; os anteriores depois de discordar ou cancelar |
| `GET /processos/{id}/alteracao-proposta` | `200 {"valor": n, "favorecidos": [{nome, documento, papel, valor}], "motivo": "..."}` enquanto AGUARDANDO_CONCORDANCIA; `404` sem proposta pendente (qualquer outra situação, inclusive depois de concordar, discordar ou cancelar) e para processo inexistente |
| `GET /processos/{id}/historico` | ações novas `ALTERAR`, `PROPOR_ALTERACAO`, `CONCORDAR`, `DISCORDAR`; cada item ganha `valorAnterior` e `valorNovo` (números, comparados como `BigDecimal`): preenchidos em `ALTERAR` e `PROPOR_ALTERACAO` (anterior = valor vigente antes; novo = valor pedido), nulos nas demais. Motivo de `PROPOR_ALTERACAO` e `DISCORDAR` em `observacao`. Recusada não entra |
| `GET /processos/{id}/acoes` | inclui `alterar` em ABERTO, `propor-alteracao` em EM_ANALISE e, em AGUARDANDO_CONCORDANCIA, exatamente `concordar`, `discordar`, `cancelar` |
| `GET /processos?estado=AGUARDANDO_CONCORDANCIA` | filtra a situação nova |

### Pagamento como saga (fatia 5)

Decidido por quem coordena a implementação; os testes usam só isto. Papéis e identidade: ver "Identidade, papéis e autor (fatia 7)". Situação nova: `PAGAMENTO_SOLICITADO`.

| Operação | Contrato |
|---|---|
| `POST /processos/{id}/solicitar-pagamento` | corpo opcional `{"observacao"}`, como as ações sem dado. Só em APROVADO: `200 {"estado": "PAGAMENTO_SOLICITADO"}`. Não depende de a tesouraria estar disponível e não envia nada a ela (R7). Fora de APROVADO: `409`. Processo inexistente: `404`. JSON malformado: `400`; corpo com tipo que não é JSON: `415` (como as demais ações com corpo) |
| `POST /processos/{id}/pagar` | não existe mais: `404` |
| Rotina | não há endpoint para disparar a rotina nem para a tesouraria nos avisar. A rotina roda sozinha, por tempo; no ambiente de teste o intervalo é de 1 segundo, pela propriedade `pagamento.rotina.intervalo` (`1s`), que o `TesourariaStubResource` devolve junto com a URL da tesouraria (vale no `*Test` e no `*IT`) |

| Efeito observável | Contrato |
|---|---|
| `GET /processos` | `estado` PAGAMENTO_SOLICITADO (filtro `estado=PAGAMENTO_SOLICITADO` também); `protocoloPagamento` nulo até PAGO e depois o protocolo da tesouraria |
| `GET /processos/{id}/acoes` | APROVADO oferece `solicitar-pagamento` no lugar de `pagar`; PAGAMENTO_SOLICITADO: lista vazia |
| `GET /cotas/{id}` | `creditoDisponivel` só cai quando o processo é PAGO |
| `GET /processos/{id}/historico` | ações novas: `SOLICITAR_PAGAMENTO` (APROVADO para PAGAMENTO_SOLICITADO); `CONFIRMAR_PAGAMENTO` (para PAGO); `RECUSAR_PAGAMENTO` (para APROVADO, `observacao` = motivo dado pela tesouraria); `TENTAR_PAGAMENTO` (de PAGAMENTO_SOLICITADO para PAGAMENTO_SOLICITADO, uma entrada por tentativa sem resultado aplicado: indisponível, sem resposta ou resposta incoerente; `observacao` não vazia descrevendo o fato, texto livre: os testes só conferem que não é vazia). As tentativas se repetem a cada intervalo: os testes conferem "ao menos N entradas `TENTAR_PAGAMENTO`" e a ordem relativa, nunca o total exato. O autor das entradas da rotina ("sistema") está em "Identidade, papéis e autor (fatia 7)" |

Espera: os testes esperam o efeito da rotina consultando a API em laço (a cada 100 ms, até 30 segundos, `aguardar` em `ApiAceitacao`), sem `Thread.sleep` fixo como asserção. O estado do processo é seguido pela última entrada do histórico (consulta por processo, barata). O sistema pode ter proteção (circuito aberto por alguns segundos depois de falhas seguidas): por isso os 30 segundos.

#### Contrato da tesouraria usado pelo stub

Sistema externo: só o `TesourariaStub` o conhece, e a chave de configuração da URL (`quarkus.rest-client.tesouraria.url`) só o `TesourariaStubResource`. Decidido por quem coordena a implementação.

| Item | Contrato |
|---|---|
| Pedido | `POST /ordens-pagamento` com JSON `{"requestId": "<identificação da solicitação, texto não vazio>", "processId": <id do processo>, "amount": <valor>, "payees": [{"name", "document", "role", "amount"}]}` |
| Confirmou | `200 {"status": "ACCEPTED", "protocolNumber": "..."}` |
| Recusou | `200 {"status": "REJECTED", "reason": "..."}` |
| Já fez a ordem | `200 {"status": "ALREADY_PROCESSED", "protocolNumber": "..."}`: vale como confirmação (R31) |
| Indisponível ou sem resposta | resposta 5xx, ou demora acima de 2 segundos (o stub demora 3) |
| Incoerentes | `ACCEPTED` sem `protocolNumber`; `REJECTED` sem `reason` |

Verbos de negócio do stub: `confirmar(protocolo)`, `recusar(motivo)`, `jaProcessou(protocolo)`, `ficarIndisponivel()`, `demorar()`, `confirmarSemProtocolo()`, `recusarSemMotivo()`, `confirmarComAtraso(duração, protocolo)` e, só para um processo (vale antes do comportamento geral), `confirmarProcesso(id, protocolo)` e `recusarProcesso(id, motivo)`. Consultas ao que a tesouraria RECEBEU (journal do WireMock, sempre filtrado por `processId`): `ordensRecebidas(id)` (identificação, valor e partes), `quantidadeDeOrdens(id)`, `identificacoesRecebidas(id)`. O estado do stub é compartilhado e a rotina roda sobre todos os processos em pagamento solicitado: cada caso começa e termina com o stub confirmando, filtra o journal pelo próprio processo e nunca depende de contagem global. Quando o caso precisa ver o processo PARADO em pagamento solicitado, deixa o stub indisponível ANTES de solicitar.

### Alçada (fatia 6)

Decidido por quem coordena a implementação; os testes usam só isto. Papéis e R17 (a segunda pessoa): ver "Identidade, papéis e autor (fatia 7)". Situação nova: `AGUARDANDO_ALCADA`. A alçada é configuração da aplicação (padrão R$ 50.000,00); os testes usam o padrão e não a alteram.

| Operação | Contrato |
|---|---|
| `POST /processos/{id}/aprovar-na-alcada` | corpo opcional `{"observacao"}`, como as ações sem dado. Só em AGUARDANDO_ALCADA: `200 {"estado": "APROVADO"}`. Fora dela: `409` e nada muda. Processo inexistente: `404`. JSON malformado: `400`; corpo com tipo que não é JSON: `415` |
| `concluir-analise` (serviço) e `concluir-vistoria` (imobiliário) | `200` com `{"estado": "APROVADO"}` se o valor VIGENTE é até R$ 50.000,00, inclusive, ou `{"estado": "AGUARDANDO_ALCADA"}` se é maior (R16). No imobiliário, `concluir-analise` responde sempre `AGUARDANDO_VISTORIA`, qualquer que seja o valor; só `concluir-vistoria` decide. A decisão é tomada em cada conclusão, com o valor vigente daquele momento |
| `reprovar`, `devolver-para-analise`, `cancelar` | já existem, com motivo obrigatório (mesmas regras de motivo e de status); passam a valer em AGUARDANDO_ALCADA: REPROVADO, EM_ANALISE e CANCELADO |

| Efeito observável | Contrato |
|---|---|
| `GET /processos/{id}/historico` | ação nova `APROVAR_NA_ALCADA` (de AGUARDANDO_ALCADA para APROVADO; `observacao` = a observação informada). Em `CONCLUIR_ANALISE` e `CONCLUIR_VISTORIA` o campo `para` é a situação a que o processo foi: APROVADO ou AGUARDANDO_ALCADA. `reprovar`, `devolver-para-analise` e `cancelar` em AGUARDANDO_ALCADA registram `de` = AGUARDANDO_ALCADA e o motivo em `observacao` |
| `GET /processos/{id}/acoes` | em AGUARDANDO_ALCADA, exatamente `aprovar-na-alcada`, `reprovar`, `devolver-para-analise`, `cancelar`; em APROVADO, o que já havia (`aprovar-na-alcada` não aparece fora de AGUARDANDO_ALCADA) |
| `GET /processos?estado=AGUARDANDO_ALCADA` | filtra a situação nova |
| `GET /processos` (campo `valor`) e `GET /cotas/{id}` | em AGUARDANDO_ALCADA o valor é o vigente; o `creditoDisponivel` não cai (só cai em PAGO) e a cota segue indisponível para novo processo (R6) |

### Identidade, papéis e autor (fatia 7)

Decidido por quem coordena a implementação; os testes usam só isto. O caso de uso não define como a pessoa se identifica nem trata o cadastro de clientes e cotas: o que vem dessas duas lacunas é decisão do coordenador, marcada abaixo.

| Item | Contrato |
|---|---|
| Identidade | `Authorization: Bearer <token JWT>` em todo pedido, menos `/q/openapi` (aberto). Claims: `sub` = usuário (texto); `groups` = papéis, entre `cliente`, `backoffice`, `aprovador`, `tesouraria` (uma pessoa pode ter mais de um); `clienteId` = id do cadastro do cliente (número), presente quando o papel é `cliente`; `iss` = `https://exemplo-negocial`; `exp` |
| Confiança no token (ambiente de teste) | A aplicação confia numa chave pública de TESTE: `mp.jwt.verify.publickey.location` (arquivo com a cópia de `src/test/resources/aceitacao/chaves/publica-teste.pem`) e `mp.jwt.verify.issuer=https://exemplo-negocial`, entregues pelo `TesourariaStubResource` (a mesma entrega vale no `*Test` e no `*IT`). Os tokens são assinados com a privada de teste (`privada-teste.pem`, RS256) em `IdentidadeTeste`, só com o JDK: a biblioteca `smallrye-jwt-build` não é necessária no classpath de teste. A aplicação, no entanto, precisa de uma extensão que confira JWT (por exemplo `quarkus-smallrye-jwt`) |
| 401 | sem token, token com assinatura de outra chave, expirado, de outro emissor ou que não é um token. Vem antes de tudo (400, 403, 404, 409) |
| 403 | token válido sem o papel da ação; cliente que não é o dono da cota (abrir, alterar, concordar, discordar); R17 (aprovar na alçada quem concluiu a análise). O papel é conferido antes de olhar o corpo e o processo: 403 de papel vem antes de 400, 404 e 409. O 403 de "cliente de outra cota" e o de R17 dependem do processo: vêm depois do 404 e da conferência do papel (a ordem entre eles e o 409 não é testada, dúvida F7-2) |
| Corpo da recusa | formato de erro de sempre (`mensagem`) quando houver corpo; os testes conferem o status e, se o corpo não vier vazio, a `mensagem` |
| Dono da cota | é o cliente cujo `clienteId` do token é o da cota informada (abrir) ou da cota do processo (alterar, concordar, discordar); o usuário (`sub`) não conta |
| Papel por ação | O quadro "Quem pode executar cada ação" (Atores): iniciar análise, solicitar e receber documento, concluir análise, concluir vistoria, propor alteração: `backoffice`; reprovar: `backoffice` em análise e `aprovador` em aguardando alçada; aprovar na alçada: `aprovador`; devolver para análise: `backoffice` (de aprovado e de aguardando vistoria) e `aprovador` (de aguardando alçada); solicitar pagamento: `tesouraria`; cancelar: `backoffice` ou `aprovador`; abrir, alterar em aberto, concordar e discordar: `cliente`, o dono da cota. A ação fora da situação do papel (por exemplo, o aprovador reprovar em análise) é recusada por papel, 403, com o processo na situação em que a ação vale |
| Cadastros | `POST /clientes` e `POST /cotas`: papel `backoffice` (decisão do coordenador; o caso de uso não trata cadastro). Sucesso é qualquer 2xx, como nas fases anteriores |
| Consultas | `GET /processos...`, `GET /cotas/{id}`: qualquer papel autenticado; sem token, 401 |
| R17 | em AGUARDANDO_ALCADA, `aprovar-na-alcada` de quem fez a ÚLTIMA conclusão de análise (`concluir-analise`) é 403, mesmo com o papel `aprovador`; a comparação é por usuário (`sub`). No imobiliário, quem concluiu só a vistoria aprova. Depois de `devolver-para-analise` e nova conclusão, vale o usuário da nova conclusão. A recusa não muda nada: situação, ações, histórico, dados vigentes, cota |
| `GET /processos/{id}/historico` | cada item ganha `autor` (texto): o `sub` do token de quem pediu; `"sistema"` nas entradas da rotina (`CONFIRMAR_PAGAMENTO`, `RECUSAR_PAGAMENTO`, `TENTAR_PAGAMENTO`). Pedido recusado não entra |

O que os auxiliares de teste enviam por padrão (`ApiAceitacao`), para os ~990 casos anteriores continuarem iguais: cadastros e consultas, backoffice "ana"; abrir, alterar, concordar e discordar, o cliente dono da cota (token com o `clienteId` do cliente criado por `novaCota`); iniciar análise, solicitar e receber documento, concluir análise e vistoria, propor alteração e cancelar, backoffice "ana"; aprovar na alçada, aprovador "bruno" (pessoa diferente de quem concluiu a análise, R17); reprovar e devolver para análise, "carla" (backoffice e aprovador, porque o papel certo depende da situação e o auxiliar não a consulta); solicitar pagamento, tesouraria "tereza". Os testes de papéis usam as variantes `...Como`, que recebem o cabeçalho (ou `IdentidadeTeste.SEM_TOKEN`).

### Que continua como hoje (testes mantidos)

| Operação | Entrada e resposta |
|---|---|
| `POST /processos` | ver "Valor e crédito (fatia 2)" e "Favorecidos (fatia 3)" acima |
| `pagar` | deixou de existir na fatia 5 (ver "Pagamento como saga (fatia 5)") |
| Pré-condições de cadastro | `POST /clientes` e `POST /cotas` (com `valorCredito`), só para criar dados próprios de cada caso. Todas as chamadas levam token (fatia 7). Os auxiliares abrem processo com o valor padrão R$ 30.000,00, dentro do crédito padrão de R$ 250.000,00, e (fatia 3) com UM favorecido válido que recebe o valor inteiro. O valor padrão está abaixo da alçada (fatia 6): os caminhos até aprovado não mudam. Processos de valor acima da alçada (por exemplo, os cenários de crédito de R$ 100.000,00 e R$ 250.000,00) passam por aguardando alçada e por aprovar na alçada |

## Modelo

Máquina de estados de negócio da **fatia 1** (o que não está listado numa situação é recusado, R5, com 409). Implementada em `ModeloPagamentoProcesso`, que gera os casos.

| Situação | Serviço | Imobiliário |
|---|---|---|
| ABERTO | iniciar análise → EM_ANALISE; cancelar* → CANCELADO | igual |
| EM_ANALISE | solicitar documento* → PENDENTE_DOCUMENTO; reprovar* → REPROVADO; concluir análise → APROVADO; cancelar* → CANCELADO | igual, mas concluir análise → AGUARDANDO_VISTORIA |
| PENDENTE_DOCUMENTO | receber documento → EM_ANALISE; cancelar* → CANCELADO | igual |
| AGUARDANDO_VISTORIA | não existe | concluir vistoria → APROVADO; devolver para análise* → EM_ANALISE; cancelar* → CANCELADO |
| APROVADO | solicitar pagamento → PAGAMENTO_SOLICITADO (fatia 5; era `pagar → PAGO`); devolver para análise* → EM_ANALISE; cancelar* → CANCELADO | igual |
| PAGAMENTO_SOLICITADO (fatia 5) | nenhuma ação de pessoa é aceita; só a rotina tira o processo dali: tesouraria confirmou → PAGO; tesouraria recusou → APROVADO | igual |
| PAGO, REPROVADO, CANCELADO | final: toda ação é recusada | igual |

\* ação com motivo obrigatório. As demais ações com corpo (iniciar análise, concluir análise, concluir vistoria, receber documento, e a partir da fatia 5 solicitar pagamento) têm observação opcional.

Critérios aplicados:

- Máquina de estados: todas as transições (cada transição usada ao menos uma vez, em caminhos de ABERTO a uma situação final, sem repetir transição dentro de um caminho: 43 caminhos); cada situação final alcançada (PAGO, REPROVADO e CANCELADO, nos dois tipos); cada ação inválida em cada situação (110 pares: 52 no serviço e 58 no imobiliário, com a situação e o histórico conferidos depois).
- Particionamento do motivo: ausente (objeto sem o campo, e corpo ausente), vazio, só espaços, 1, 500 e 501 caracteres (e 500 acentuados, para contar caracteres e não bytes).
- Particionamento da observação: ausente (`{}` e sem corpo), curta, 500 caracteres.
- Tabela de decisão da cota (R6): cada situação em andamento recusa; cada situação final libera; reprovado conferido à parte.
- Cobertura de terminais: as nove ações do caminho, os campos `motivo` e `observacao`, os campos do histórico, os filtros `estado` novos, e os status 200, 400, 404, 409, 415 e 503.
- Suposição de erro: JSON malformado e tipo de conteúdo que não é JSON em toda ação que recebe corpo.

### Modelo de valor e crédito (fatia 2)

Em `ModeloPagamentoProcesso`: cada cenário é uma cota com um crédito e uma sequência de processos, cada um levado a uma situação pelos caminhos da máquina de estados (só o último pode estar em andamento, R6). O oráculo calcula `creditoDisponivelEsperado = crédito − soma dos valores dos processos PAGOS` (R13, R14).

- Cenários com a cota livre (12): sem processo; pago 100.000 (serviço e imobiliário); dois pagos (100.000 e 50.000); cancelado 100.000 (os dois tipos); reprovado 100.000 (os dois tipos); pago + cancelado + reprovado; pago de 0,01; crédito 1.000,50 com pago 500,25; crédito esgotado.
- Cenários com processo em andamento (10): um para cada situação em andamento nos dois tipos (9) e pago 100.000 mais outro de 60.000 em análise.
- Critérios: particionamento do valor (válidos: 0,01, 100, 100,5, 12.500,50, exatamente o crédito; inválidos: ausente, nulo, 0, 0,00, -0,01, -100, 3 casas em 100,005, 0,001 e 100,123); valor-limite do crédito (exatamente o disponível aceito, um centavo acima recusado, em cada cenário com a cota livre); tabela de decisão do crédito (pago consome; andamento, reprovado, cancelado e processo de outra cota não); 400 antes de 404 e de 409.

### Modelo de favorecidos (fatia 3)

Em `ModeloPagamentoProcesso`: cada caso é um pedido de abertura (valor do processo e lista de favorecidos, com campos ausentes, nulos ou em branco quando o caso o exige). O oráculo `CasoFavorecidos.resultado()` aplica R15, R29 e o contrato e devolve `ACEITO` (201 e a consulta devolve os favorecidos informados, na ordem, somando o valor), `RECUSA_DE_FORMATO` (400 com `campos` de favorecidos) ou `RECUSA_DE_REGRA` (400 com `mensagem`); formato é decidido antes de regra, e cada caso tem um único defeito.

- Particionamento e valor-limite: 1, 2, 10 e 100 favorecidos; CPF e CNPJ; parte de 0,01; partes sem casas, com uma e com duas casas; nome e papel com 1, 120 e 50 caracteres (e acentuados) e com 121 e 51; documento com 1, 10, 12, 13 e 15 dígitos, com pontuação, com letra, com espaço e com sinal; parte ausente, nula, zero, 0,00, negativa e com três casas; soma um centavo abaixo e um centavo acima, em 1, 2, 3 e 10 favorecidos; defeito no primeiro e no último favorecido.
- Rateio com centavos: 33,34 + 33,33 + 33,33 = 100,00 aceito; 33,33 x 3 (99,99) e 33,34 x 3 (100,02) recusados.
- Tabela de decisão de R15 e R29 (seis linhas): o caso-base aceito, e cada condição sozinha recusa: ao menos um favorecido, dados completos, parte maior que zero, soma exata, sem repetição.
- Documento repetido: mesmo CPF e mesmo CNPJ, com nome e papel iguais ou diferentes, com partes iguais, no primeiro e no terceiro de três, três vezes e no primeiro e no décimo de dez; mesmo nome e papel com documentos diferentes é aceito (identidade é o documento, dúvida F3-4).
- Precedência: quatro recusas de formato (lista ausente, lista vazia, documento com 10 dígitos, parte 0,00) contra cota inexistente (404), cota com processo em andamento (409) e valor um centavo acima do crédito (409).
- Cobertura de terminais: `favorecidos`, `nome`, `documento`, `papel` e `valor` do favorecido; `POST /processos` 201 e 400; `GET /processos/{id}/favorecidos` 200 e 404.
- Efeito observável nos outros endpoints: a consulta de favorecidos, a listagem (valor) e a cota (crédito disponível inalterado); depois de recusa, a contagem de processos da cota e uma abertura válida em seguida.
- Consulta: ordem informada (quatro favorecidos fora de ordem de nome, documento e parte); processos diferentes não se confundem; os favorecidos continuam os mesmos em cada uma das 15 situações dos dois tipos.

### Modelo de alteração (fatia 4)

Em `ModeloPagamentoProcesso`. A máquina de estados ganha a situação AGUARDANDO_CONCORDANCIA e quatro ações (o oráculo de ações aceitas por situação foi estendido; as ações antigas ficam como estavam):

| Situação | Transições novas (serviço e imobiliário) |
|---|---|
| ABERTO | alterar → ABERTO |
| EM_ANALISE | propor alteração* → AGUARDANDO_CONCORDANCIA |
| AGUARDANDO_CONCORDANCIA | concordar → EM_ANALISE; discordar* → EM_ANALISE; cancelar* → CANCELADO; toda outra ação é recusada (R28) |

\* motivo obrigatório. `alterar` e `propor-alteracao` levam também valor e favorecidos.

`ProcessoEsperado` é o oráculo dos dados VIGENTES: valor e favorecidos que valem, proposta pendente (valor, favorecidos, motivo) e histórico esperado (ação, valor anterior, valor novo). O teste pede a ação e só atualiza o oráculo depois do 200: alterar muda o vigente na hora; propor só guarda a proposta; concordar promove a proposta; discordar e cancelar a descartam. A cada passo dos fluxos confere-se, contra o oráculo, a situação e o valor (listagem), os favorecidos, o crédito disponível da cota (o do crédito menos o valor vigente, só quando PAGO), a proposta (200 ou 404) e, no fim, o histórico.

- Máquina de estados: os caminhos completos que não usam ação de alteração continuam os 43 de antes (`fluxoChegaASituacaoFinal`). Os que usam são centenas (o grafo ganhou ciclos); `fluxoComAlteracaoChegaASituacaoFinal` roda um conjunto mínimo (guloso) por tipo, que usa cada transição nova e chega a cada final depois de uma alteração (6 caminhos). Caminhos escolhidos à mão: propor, discordar, propor de novo, concordar; cinco rodadas sem limite; propor e cancelar; alterar quatro vezes em aberto; alterar, propor e responder (concordar ou discordar) e seguir até PAGO conferindo o crédito consumido; resposta repetida ou cruzada.
- Tabela situação × ação: as quatro ações novas e o cancelar entram nas 13 ações do modelo e em todas as situações dos dois tipos (`acaoForaDaOrdemERecusada`, com 76 pares novos; transições aceitas nos fluxos). Em AGUARDANDO_CONCORDANCIA, cada ação recusada é conferida também quanto à proposta, que continua a mesma.
- Valor-limite: exatamente o crédito disponível aceito e um centavo acima recusado (409), em `alterar` e em `propor-alteracao`, em quatro cenários de crédito (cota sem processo, pago, dois pagos, crédito com centavos); soma um centavo a menos e a mais.
- Particionamento: valor (válidos 0,01, duas casas e igual ao crédito; inválidos ausente, nulo, zero e três casas) e favorecidos (amostra de cada classe de R15 e R29: lista ausente e vazia, nome vazio, documento com 10 dígitos, papel com 51, parte nula, zero e com três casas, soma a menos e a mais, rateio, documento repetido) nas duas vias (cliente em aberto, backoffice em análise); tabela de decisão de R15 e R29 (6 linhas) nas duas vias; motivo (ausente, vazio, só espaços, 1, 500 e 501 caracteres, e 500 acentuados) em propor e discordar. Depois de cada recusa, um pedido válido é aceito (a recusa não travou o processo).
- Precedência: 400 de formato (favorecidos, valor, motivo) antes de 404, de 409 de situação e de 409 de crédito.
- Cobertura de terminais: `alterar`, `propor-alteracao`, `concordar`, `discordar`, `alteracao-proposta` (200 e 404), `valorAnterior`, `valorNovo`, a situação nova na listagem e nas ações.
- Suposição de erro: JSON malformado (400) e tipo que não é JSON (415) nas quatro ações novas; concordar e discordar ao mesmo tempo (um vale).
- Oráculo antigo ajustado: `acoesAceitasNaSituacao` agora espera `alterar` em ABERTO e `propor-alteracao` em EM_ANALISE (compara sem depender da ordem, como antes); os testes de motivo (`motivoAusenteERecusado`, `motivoInvalidoERecusado`, `motivoValidoFicaNoHistorico`) montam o corpo pela ação (em propor, com valor e favorecidos válidos, para o motivo ser o único defeito); `consultarHistorico` lê números como BigDecimal. As verificações não mudaram.

### Modelo de pagamento como saga (fatia 5)

Em `ModeloPagamentoProcesso`. A máquina de estados ganha a situação PAGAMENTO_SOLICITADO e troca `pagar` por `solicitar-pagamento` (13 ações de pessoa). "Rotina: tesouraria confirmou" (→ PAGO) e "rotina: tesouraria recusou" (→ APROVADO) são **transições do modelo, não ações de pessoa**: entram nos caminhos, mas nunca nas ações aceitas, recusadas ou consultadas. PAGAMENTO_SOLICITADO está em andamento (R6): a cota segue indisponível e o crédito só é consumido em PAGO.

| Situação | Transições novas (serviço e imobiliário) |
|---|---|
| APROVADO | solicitar pagamento → PAGAMENTO_SOLICITADO |
| PAGAMENTO_SOLICITADO | toda ação de pessoa recusada (R20); rotina confirmou → PAGO; rotina recusou → APROVADO; rotina sem resposta, indisponível ou incoerente: continua, e cada tentativa vai ao histórico |

Como o teste conduz o processo (`levar`): ao solicitar, a tesouraria já fica como o próximo passo do caminho manda (confirma, recusa) ou, se o caminho termina em PAGAMENTO_SOLICITADO, fora do ar; "rotina confirmou" e "rotina recusou" são esperas pelo efeito, em laço. Nos caminhos de `fluxoChegaASituacaoFinal` a tesouraria fica fora do ar até o pagamento solicitado ser conferido, e só então confirma ou recusa.

Critérios aplicados:

- Máquina de estados: todas as transições (os caminhos sem ação de alteração passam de 43 para 61: 26 no serviço e 35 no imobiliário, com 22 passos da rotina); cada situação final alcançada (PAGO por confirmação da rotina); cada ação inválida em cada situação (a coluna "solicitar pagamento" em toda situação, e a linha PAGAMENTO_SOLICITADO com as 13 ações de pessoa recusadas, nos dois tipos: 212 pares em `acaoForaDaOrdemERecusada`).
- Tabela de decisão da resposta da tesouraria × efeito (`Resposta`): confirmou, já processou, recusou, indisponível, demorou, confirmação sem protocolo e recusa sem motivo; o efeito conferido é situação, protocolo, histórico, crédito, ações oferecidas e cota (livre só em PAGO). Serviço em todas as respostas e imobiliário em confirmou e recusou.
- Sequências: indisponível por três tentativas e depois confirma (mesma identificação, nos dois tipos); sem resposta e depois "já fiz essa ordem"; confirmação sem protocolo e depois confirmação; recusa sem motivo e depois recusa com motivo; recusa, nova solicitação, recusa, nova solicitação, confirmação (três identificações diferentes); recusa e depois devolver para análise ou cancelar; duas cotas ao mesmo tempo, cada uma com seu resultado (resposta por processo) e com identificações diferentes.
- Dados enviados: valor e partes vigentes depois de alterações do cliente em aberto e depois de concordar com a proposta do backoffice; identificação não vazia; `processId` do processo.
- Cobertura de terminais: `solicitar-pagamento` (200, 400, 404, 409, 415), `pagar` (404), `protocoloPagamento`, as quatro ações novas do histórico, o filtro `estado=PAGAMENTO_SOLICITADO`, `acoes` em APROVADO e em PAGAMENTO_SOLICITADO.
- Suposição de erro: solicitações simultâneas (uma vale), a rotina que só olha processos em PAGAMENTO_SOLICITADO (um processo em pagamento solicitado serve de relógio: três pedidos dele são três rodadas), JSON malformado e tipo que não é JSON na ação nova.
- Ajustes nos casos antigos: os que chegavam a PAGO por `pagar` chegam por `solicitar-pagamento` mais a espera da rotina; o histórico dos fluxos é conferido sem as entradas `TENTAR_PAGAMENTO` (que não têm número certo); a observação opcional (`observacaoOpcionalAceitaObjetoVazio`, `observacaoFicaNoHistorico`) passa a valer também para `solicitar-pagamento`, com a tesouraria fora do ar para o processo ficar em pagamento solicitado.
- Amostragem por custo (cada processo pago custa 1 a 2 segundos de espera da rotina): o valor-limite do crédito na abertura usa 7 dos 12 cenários de cota livre (sem processo, um pago, dois pagos, cancelado, reprovado, crédito com centavos, crédito esgotado), e na alteração 3 cenários (sem processo, um pago, crédito com centavos). Os 26 cenários de `creditoDisponivelConsideraSoOsPagos` continuam inteiros.

### Modelo de alçada (fatia 6)

Em `ModeloPagamentoProcesso`. A máquina de estados ganha a situação AGUARDANDO_ALCADA e a ação `aprovar-na-alcada` (14 ações de pessoa). A transição de concluir (a análise no serviço, a vistoria no imobiliário) passa a ser **condicional ao valor vigente**: o modelo a trata com duas faixas de valor (`Faixa`), "até a alçada" (valor padrão R$ 30.000,00: concluir leva a APROVADO) e "acima da alçada" (R$ 60.000,00: concluir leva a AGUARDANDO_ALCADA). `decisaoDaAlcada(valor)` é o oráculo (até R$ 50.000,00, inclusive, aprovado; acima, aguardando alçada). Cada caminho tem a faixa do processo que o percorre e o valor dele não muda no caminho; por isso, na faixa acima da alçada, `alterar` e `concordar` (que mudam o valor vigente) não entram nos caminhos. A decisão depois de uma alteração de valor é conferida à mão.

| Situação | Transições novas (serviço e imobiliário) |
|---|---|
| EM_ANALISE (serviço) | concluir análise: valor até a alçada → APROVADO; acima → AGUARDANDO_ALCADA |
| AGUARDANDO_VISTORIA (imobiliário) | concluir vistoria: valor até a alçada → APROVADO; acima → AGUARDANDO_ALCADA. Concluir a análise no imobiliário leva sempre a AGUARDANDO_VISTORIA |
| AGUARDANDO_ALCADA | aprovar na alçada → APROVADO; reprovar* → REPROVADO; devolver para análise* → EM_ANALISE; cancelar* → CANCELADO; toda outra ação é recusada (R5) |

\* motivo obrigatório. `aprovar-na-alcada` tem observação opcional. AGUARDANDO_ALCADA está em andamento (R6): a cota segue indisponível e o crédito só é consumido em PAGO.

Critérios aplicados:

- Máquina de estados: todas as transições novas em caminhos de ABERTO a uma situação final (`fluxoPelaAlcadaChegaASituacaoFinal`: 8 caminhos, 4 por tipo, escolhidos por conjunto mínimo guloso, dos mais curtos aos mais longos: usam a decisão de concluir que vai a aguardando alçada, cada uma das quatro saídas dela e chegam a PAGO, REPROVADO e CANCELADO depois dela; só o caminho que chega a PAGO espera a rotina); cada ação inválida em cada situação: a coluna "aprovar na alçada" em todas as 19 situações dos dois tipos e a linha AGUARDANDO_ALCADA inteira (10 ações recusadas por tipo), em `acaoForaDaOrdemERecusada` (251 pares, 39 novos).
- Valor-limite da alçada (R16), nos dois tipos: 49.999,99 e 50.000,00 (aprovado), 50.000,01, 100.000,00 e 250.000,00 (aguardando alçada); no imobiliário, a análise leva a AGUARDANDO_VISTORIA em todos e a vistoria decide (`concluirDecidePelaAlcada`: 10 casos). O mesmo limite depois de alterar em aberto (para 50.000,00 aprovado, para 50.000,01 aguardando alçada).
- Tabela de decisão da alçada com valor vigente (R16, D35): quatro linhas de alteração do cliente em aberto (60.000 para 40.000, 40.000 para 60.000, e os dois limites) e quatro linhas de proposta do backoffice (40.000 para 60.000 e 60.000 para 40.000, com concordo e com discordo): a decisão acompanha o valor vigente ao concluir, e uma proposta só conta se o cliente concorda. Nos dois tipos (16 casos).
- Decide de novo: devolver de aguardando alçada e concluir de novo, sem mudar o valor, volta a aguardando alçada (no imobiliário a vistoria se repete); devolver e mudar o valor com a concordância do cliente refaz a decisão nos dois sentidos, inclusive a partir de aprovado (R23).
- Efeito de cada saída de aguardando alçada: aprovar na alçada segue por solicitar pagamento até PAGO, com o crédito consumido só no pago e a cota livre; reprovar (motivo "favorecido divergente") e cancelar são finais, não consomem crédito e liberam a cota (R14); devolver volta a em análise com a cota indisponível. Conferidos pelo histórico (só as entradas novas, a partir do tamanho anterior), pela consulta de ações, pela listagem, pela cota e pelo crédito.
- Cobertura de terminais: `aprovar-na-alcada` (200, 400, 404, 409, 415), os destinos de concluir, `APROVAR_NA_ALCADA` e o `para` das conclusões no histórico, o filtro `estado=AGUARDANDO_ALCADA`, `acoes` em AGUARDANDO_ALCADA.
- Suposição de erro: aprovar na alçada e reprovar ao mesmo tempo (um vale); JSON malformado e tipo que não é JSON na ação nova.
- Ajustes nos casos antigos: os casos de crédito com processos de R$ 100.000,00 e R$ 250.000,00 até aprovado, pago ou pagamento solicitado agora passam por aguardando alçada e aprovar na alçada (o caminho é escolhido pela faixa do valor, `caminhoAte(tipo, situacao, valor)`); os caminhos por situação (`processoEm`) abrem o processo com o valor da faixa do caminho; `favorecidosPermanecemEmCadaSituacao` usa favorecidos que somam R$ 60.000,00 em aguardando alçada; `listagemFiltraPorSituacoesNovas`, `acoesAceitasNaSituacao`, `cotaComProcessoEmAndamentoNaoAbreOutro`, `naoHaPropostaForaDeAguardandoConcordancia` e os casos de motivo e observação ganham a situação nova pelo modelo. Nenhuma asserção antiga mudou.

### Modelo de papéis e autoria (fatia 7)

Em `ModeloPapeis` (a tabela papel × ação) e em `PapeisEAutoriaAceitacaoTest`. Quem pede é uma `Credencial`: o cliente dono da cota, um cliente de outra cota, backoffice ("ari"), aprovador ("bia"), tesouraria ("tereza"), um usuário válido sem papel ("visitante") e o pedido sem token. Pessoas dos auxiliares ("ana", "bruno", "carla") e dos testes de R17 ("dora" também com os dois papéis) vêm de `IdentidadeTeste`.

| Linha da tabela | Quem é aceito |
|---|---|
| iniciar análise (aberto), solicitar documento (em análise), receber documento (pendente), concluir análise (serviço e imobiliário), concluir vistoria, propor alteração (em análise) | backoffice |
| reprovar em análise; devolver de aprovado e de aguardando vistoria | backoffice |
| reprovar, devolver e aprovar na alçada, em aguardando alçada | aprovador |
| solicitar pagamento (aprovado) | tesouraria |
| cancelar, em cada situação não final que aceita a ação (aberto, em análise, pendente de documento, aguardando vistoria, aguardando alçada, aguardando concordância, aprovado) | backoffice e aprovador |
| alterar (aberto), concordar e discordar (aguardando concordância) | cliente dono da cota |

Critérios aplicados:

- Tabela de decisão papel × ação (`acaoSoEAceitaDeQuemTemOPapel`): 24 linhas (ação na situação em que vale) × 7 credenciais = 168 casos. Aceito: 200, destino certo, e uma entrada nova no histórico com a ação, `de`, `para` e o `autor` igual ao usuário do token. Recusado: 403 (401 sem token), `mensagem` quando há corpo, e nada muda (histórico inteiro, ações, favorecidos, proposta, crédito, situação). Cada papel e a falta de token estão em toda linha.
- Abrir processo (`aberturaSoEAceitaDoClienteDaCota`, 7 casos): dono aceito (201, e é o único processo da cota); cada outra credencial recusada, com nada criado e a cota livre (o dono abre depois). Dono pelo `clienteId` do token, não pelo usuário (`donoEPeloClienteIdDoToken`, serviço e imobiliário).
- Cadastros (`cadastroSoEAceitoDoBackoffice`, 12 casos): cliente e cota, backoffice aceito; cliente, aprovador, tesouraria, sem papel e sem token recusados.
- Consultas (`consultaEDeTodosOsPapeis`, 32 casos): lista, ações, histórico, favorecidos, proposta e cota, de cada papel (e do dono) com 200; sem token, 401; cliente de outra cota só na lista e na cota (dúvida F7-3).
- 401 (`identificacaoAusenteOuInvalidaE401`, 25 casos): cinco defeitos de identificação (sem token, outra chave, expirado, outro emissor, texto que não é token) × cinco operações (listar, consultar cota, iniciar análise com o processo intacto e ainda utilizável depois, abrir com nada criado, cadastrar cliente); `/q/openapi` sem token.
- Ordem das recusas: 401 antes de 404, 400 e 409 (cinco defeitos); 403 de papel antes de 404 (processo inexistente), de 400 (motivo ausente, JSON malformado, `{}`, valor zero na abertura) e de 409 (ação fora da ordem; abertura de cota inexistente e de cota em andamento); 404 antes do 403 de cliente de outra cota (alterar, concordar, discordar); usuário sem papel não escreve.
- Mais de um papel: pessoa com backoffice e aprovador exerce os dois (e o histórico guarda o usuário); cliente da cota que também é backoffice abre e inicia a análise.
- R17, tabela de decisão (papel de aprovador × ser a pessoa da última conclusão da análise), nos dois tipos: aprovador que não concluiu (200); aprovador e backoffice que não concluiu (200); aprovador e backoffice que concluiu (403); só backoffice que concluiu (403); só backoffice que não concluiu (403): 10 casos. Mais: mesma pessoa conclui e aprova (403, continua aguardando alçada, ações e cota inalteradas) e outra pessoa com os mesmos papéis aprova (200); quem só iniciou a análise aprova; imobiliário: quem concluiu só a vistoria aprova e quem concluiu a análise não, e quem concluiu as duas não; devolvida e concluída por outra pessoa, a regra passa a valer para a nova pessoa e quem concluiu na primeira vez aprova.
- Autor no histórico (R19): `autorDeCadaEntradaEQuemPediu` roda 14 caminhos completos do modelo de estados, escolhidos por conjunto mínimo guloso para usar cada ação de pessoa nos dois tipos (reprovar e devolver contadas por situação de origem) e os dois resultados da rotina; em cada passo pessoas diferentes do mesmo papel se alternam; confere-se o `autor` de cada entrada (usuário de quem pediu, "sistema" nas da rotina) na ordem do histórico, sem as tentativas, e que toda `TENTAR_PAGAMENTO` é "sistema". Mais: tentativas e confirmação da rotina com a tesouraria indisponível e depois no ar; recusa por papel e por falta de token não entra no histórico.
- Cobertura de terminais: `autor` no histórico; 401 e 403 em escrita, consulta, abertura e cadastro; `Authorization`.
- Defeitos já vistos, evitados: o histórico inteiro é comparado só quando a situação não tem a rotina em andamento (as recusas ocorrem em situações paradas), e depois de uma ação aceita só se confere "a partir da entrada anterior"; o número de pedidos à tesouraria sob erro não é contado (espera-se por entradas do histórico).

## Rastreabilidade

Casos da fatia 1: 290; fatia 7: 1286 no total, 295 casos novos, todos em `PapeisEAutoriaAceitacaoTest` (168 da tabela papel × ação, 7 de abertura, 2 de dono pelo `clienteId`, 12 de cadastros, 32 de consultas, 25 de 401, 1 de `/q/openapi`, 5 de ordem do 401, 3 de ordem do 403, 3 do 404 antes do 403 de cota, 1 de usuário sem papel, 2 de mais de um papel, 10 + 8 de R17, 14 caminhos de autoria, 2 de autor da rotina e de recusa fora do histórico; os 991 anteriores só mudaram nos auxiliares, e o teste unitário `IdentidadeTesteTest`, de 5 casos, não é de aceitação e não entra na conta); casos novos da fatia 2: 75; casos novos da fatia 3: 118; casos novos da fatia 4: 299 (total 782); fatia 6: 991 no total, 127 casos novos sobre os 864 que existiam (54 em testes novos e 73 em casos novos de testes antigos, que ganharam a situação e a ação novas pelo modelo; os 864 foram recontados e o valor 833 abaixo, da fatia 5, estava desatualizado); fatia 5: 833 no total (os 2 casos antigos de pagar foram substituídos, 5 + 5 casos de valor-limite do crédito e 4 da alteração saíram por amostragem, e as demais mudanças vêm de situação e ação novas no modelo) no `PagamentoProcessoAceitacaoTest` (o `PagamentoProcessoAceitacaoIT` herda os mesmos). "Pendente: fatia N" marca regra cuja fatia ainda não chegou, sem teste.

| Regra | Modelo e critério | Teste | Casos |
|---|---|---|---|
| R1 (parte: cota e tipo; hoje vale como antes) | Particionamento com Base Choice | `escolhaBaseAbreProcesso`, `tipoImobiliarioAbreProcesso`, `cotaInexistente`, `cotaAusente`, `tipoInvalido`, `tipoAusente`, `corpoMalformado`, `corpoVazio` | 8 |
| R1 (favorecidos obrigatórios) | Particionamento: lista ausente, nula e vazia; tabela de decisão: ao menos um favorecido | `favorecidosComFormatoInvalidoSaoRecusados` (3 casos de lista), `tabelaDeDecisaoDosFavorecidos`, `favorecidosInvalidosVemAntesDeCotaInexistente` | 3, 1, 1 |
| R1 e R18 ("só o cliente da cota abre") | Tabela de decisão: papel × credencial; ordem 403 antes de 400, 404 e 409; 401 | `aberturaSoEAceitaDoClienteDaCota`, `papelErradoVemAntesDaSituacaoEDosDadosDaAbertura`, `papelErradoVemAntesDoFormatoDoCorpo`, `donoEPeloClienteIdDoToken`, `clienteComPapelDeBackofficeExerceOsDois` | 7, 1, 1, 2, 1 |
| R2 e R3 (parte: abertura, análise, documento, vistoria, aprovação, pagamento direto) | Máquina de estados: todas as transições e cada final | `fluxoChegaASituacaoFinal` | 43 |
| R2 e R3 (aguardando concordância) | Máquina de estados: todas as transições novas, cada final depois de alteração e cada ação inválida na situação nova | `fluxoComAlteracaoChegaASituacaoFinal`, `acaoForaDaOrdemERecusada`, `proporDiscordarProporConcordar`, `acoesAceitasNaSituacao` | 6, 76 novos, 2, 2 |
| R2 e R3 (pagamento solicitado; pago pela rotina) | Máquina de estados: todas as transições (solicitar pagamento, rotina confirmou, rotina recusou), cada final e cada ação inválida | `fluxoChegaASituacaoFinal`, `fluxoComAlteracaoChegaASituacaoFinal`, `acaoForaDaOrdemERecusada`, `acoesAceitasNaSituacao`, `pagarNaoExisteMais`, `aprovadoOfereceSolicitarPagamentoNoLugarDePagar` | 61, 6, 251 (fatia 6), 21 (fatia 6), 1, 2 |
| R2 e R3 (aguardando alçada) | Máquina de estados: todas as transições novas (concluir condicional, aprovar na alçada, reprovar, devolver e cancelar em aguardando alçada), cada final depois dela e cada ação inválida na situação nova e na coluna nova | `fluxoPelaAlcadaChegaASituacaoFinal`, `acaoForaDaOrdemERecusada`, `acoesAceitasNaSituacao`, `aguardandoAlcadaOfereceQuatroAcoes`, `concluirDecidePelaAlcada` (imobiliário: a análise leva à vistoria) | 8, 39 novos, 2 novos, 2, 10 |
| R4 (parte: reprovado final; cancelar com motivo) | Máquina de estados: finais e ação inválida; particionamento do motivo | `fluxoChegaASituacaoFinal`, `acaoForaDaOrdemERecusada`, `motivoAusenteERecusado`, `corpoAusenteEmAcaoComMotivo`, `motivoInvalidoERecusado`, `motivoValidoFicaNoHistorico` | contado em cada teste |
| R4 (cancelar em aguardando concordância descarta a alteração) | Máquina de estados; motivo | `proporECancelar`, `propostaSomeDepoisDaResposta`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico` | 2, 2, 2, 2 |
| R4 (cancelar em pagamento solicitado é recusado e nada muda) | Máquina de estados: ação inválida em pagamento solicitado | `acaoForaDaOrdemERecusada`, `cancelarEDevolverEmPagamentoSolicitadoSaoRecusados` | contado, 2 |
| R4 (aprovado que teve o pagamento recusado cancela normalmente) | Sequência: recusa da tesouraria e depois cancelar | `recusaDaTesourariaNaoImpedeDevolverNemCancelar` | 4 (com devolver, R23) |
| R4 e R18 (só backoffice e aprovador cancelam; o cliente não cancela, inclusive em aguardando concordância) | Tabela papel × ação: sete situações em que cancelar vale × sete credenciais | `acaoSoEAceitaDeQuemTemOPapel` (linhas de cancelar) | 49 |
| R5 | Máquina de estados: cada ação inválida em cada situação; 404 em processo inexistente | `acaoForaDaOrdemERecusada`, `acaoEmProcessoInexistente` | 251, 14 (fatia 6) |
| R6 | Tabela de decisão: cada situação em andamento recusa, cada final libera, outra cota não impede, simultâneos | `cotaComProcessoEmAndamentoNaoAbreOutro`, `cotaComProcessoFinalizadoAbreOutro`, `reprovadoLiberaACota`, `processoDeOutraCotaNaoImpede`, `pedidosSimultaneosAbremUmUnicoProcesso` | 15 (fatia 6), 6, 1, 1, 1 |
| R7 (solicitar só leva a pagamento solicitado; não depende da tesouraria; sem protocolo; observação opcional) | Máquina de estados; cobertura de terminais; efeito pelo histórico, pela listagem e pela cota | `solicitarComTesourariaIndisponivelFicaEmPagamentoSolicitado`, `respostaDaTesourariaTemOEfeitoDoModelo`, `observacaoOpcionalAceitaObjetoVazio`, `observacaoFicaNoHistorico`, `jsonMalformadoNaAcao`, `corpoQueNaoEJsonNaAcao`, `acaoEmProcessoInexistente`, `acaoForaDaOrdemERecusada`, `pagarNaoExisteMais` | 2, 9, 15, 30, 14, 14, 14, 251 (fatia 6), 1 |
| R7 (nada é enviado à tesouraria ao solicitar) | não observável por fora: entre solicitar e a primeira rodada não há como provar ausência; prova-se que solicitar responde 200 com a tesouraria indisponível e que a rotina só olha pagamento solicitado | `solicitarComTesourariaIndisponivelFicaEmPagamentoSolicitado`, `rotinaSoOlhaProcessosEmPagamentoSolicitado` | 2, 1 |
| R8 (indisponível ou sem resposta não recusa nem paga; continua em pagamento solicitado) | Tabela de decisão da resposta; sequência | `respostaDaTesourariaTemOEfeitoDoModelo` (indisponível e demorou), `indisponivelPorAlgumasTentativasDepoisConfirma`, `semRespostaDepoisJaProcessouValeComoConfirmacao`, `respostaDentroDoPrazoEAplicada` (valor-limite do prazo) | 2, 2, 1, 1 |
| R9 | Cobertura de terminais: toda recusa traz `mensagem` | asserção de `mensagem` nos testes de recusa acima | contado em cada teste |
| R10 | Máquina de estados: solicitar documento (com motivo) e receber documento; particionamento do motivo e da observação | `fluxoChegaASituacaoFinal`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico`, `observacaoOpcionalAceitaObjetoVazio`, `observacaoFicaNoHistorico`, `acoesAceitasNaSituacao` | contado em cada teste |
| R11 (parte: reprovar em análise) | Máquina de estados: transição, final e ação inválida (inclusive reprovar em aprovado); particionamento do motivo | os mesmos de R10 e `acaoForaDaOrdemERecusada` | contado em cada teste |
| R11 (reprovar em aguardando alçada) | Máquina de estados; particionamento do motivo; efeito: final, sem crédito, cota livre | `reprovarOuCancelarEmAguardandoAlcadaEFinalELiberaACota`, `fluxoPelaAlcadaChegaASituacaoFinal`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico`, `acaoForaDaOrdemERecusada` (reprovar em aprovado e nas demais situações) | 2, contado, 2 novos, contado (a partição do motivo é a mesma nas demais situações), contado | |
| R1 (valor obrigatório, fatia 2) | Particionamento do valor: ausente | `valorInvalidoERecusado` (ausente), `valorAusenteNosDoisTipos` | 1, 2 |
| R12 (abertura) | Particionamento e valor-limite: 0,01, 2 casas aceitos; 0, negativo, 3 casas, ausente, nulo recusados com 400 e `campos` = `valor`; 400 antes de 404 e de 409 | `valorValidoAbreProcesso`, `valorInvalidoERecusado`, `valorInvalidoVemAntesDeCotaInexistente`, `valorAusenteVemAntesDeCotaInexistente`, `valorInvalidoVemAntesDeCotaIndisponivel` | 5, 9, 1, 1, 1 |
| R12 (alteração) | Particionamento do valor nas duas vias; 400 antes de 404 e de 409 | `alteracaoComValorValidoEAceita`, `alteracaoComValorInvalidoERecusada`, `valorInvalidoNaAlteracaoVemAntesDe404E409` | 6, 8, 2 |
| R13 (abertura) | Modelo de crédito: igual ao disponível aceito, um centavo acima recusado (409, nada criado, cota livre); crédito esgotado; só o pago consome | `creditoDisponivelConsideraSoOsPagos`, `valorIgualAoCreditoDisponivelEAceito`, `valorUmCentavoAcimaDoCreditoERecusado`, `exemploDoCasoDeUsoCreditoRestante`, `cotaNovaTemOCreditoInteiroDisponivel`, `creditoEsgotadoRecusaQualquerValor`, `processoDeOutraCotaNaoReduzCredito`, `pagamentoNaoConcluidoNaoConsomeCredito`, `creditoSoDiminuiNoPagamento` | 26, 6, 7, 1, 1, 1, 1, 1, 1 (valor-limite com amostra de cenários, fatia 5) |
| R13 (alteração) | Valor-limite: igual ao disponível aceito, um centavo acima recusado (409), em alterar e propor; a proposta pendente não consome crédito | `valorIgualAoCreditoDisponivelNaAlteracaoEAceito`, `valorUmCentavoAcimaDoCreditoNaAlteracaoERecusado`, `exemploDoCasoDeUsoDeCreditoNaAlteracao`, `propostaPendenteNaoConsomeCredito`, `alteracoesAteOPagamentoConsomemOCreditoDoValorFinal` | 6, 6, 1, 1, 4 (amostra de cenários, fatia 5) |
| R14 | Tabela de decisão: cancelado e reprovado não consomem e liberam a cota (e o R6 já testado) | cenários de `creditoDisponivelConsideraSoOsPagos`, `valorIgualAoCreditoDisponivelEAceito`, `cancelarLiberaCotaECredito`, `reprovarLiberaCotaECredito` | contado em cada teste, 1, 1 |
| Contrato: `valor` na listagem e `creditoDisponivel` na cota | Cobertura de terminais | `listagemMostraOValor`, `listagemMostraOValorDeCadaProcesso`, `escolhaBaseAbreProcesso`, `creditoDisponivelConsideraSoOsPagos` | 2, 1, 1, 22 |
| R15 (abertura) | Particionamento e valor-limite da soma (um centavo abaixo e acima; 1, 2, 3, 10 e 100 favorecidos; parte de 0,01); rateio com centavos; documento repetido; tabela de decisão | `favorecidosValidosAbremProcesso`, `exemploDoCasoDeUsoNosDoisTipos`, `favorecidosQueFeremARegraSaoRecusados`, `tabelaDeDecisaoDosFavorecidos`, `recusaDeFavorecidosNosDoisTipos` | 19, 2, 19, 6, 2 |
| R15 (em toda alteração) | Amostra de cada classe, soma um centavo a menos e a mais, rateio, documento repetido, tabela de decisão; recusa deixa os dados anteriores | `alteracaoComFavorecidosValidosEAceita`, `alteracaoQueFereARegraDeFavorecidosERecusada`, `tabelaDeDecisaoDosFavorecidosNaAlteracao`, `alteracaoRecusadaNosDoisTipos`, `exemploDoCasoDeUsoNaAlteracao` | 12, 10, 12, 2, 2 |
| R29 (abertura) | Particionamento: nome, documento (CPF, CNPJ, tamanhos, caracteres) e papel obrigatórios e nos limites; cobertura de terminais | `favorecidosComFormatoInvalidoSaoRecusados`, `favorecidosValidosAbremProcesso`, `tabelaDeDecisaoDosFavorecidos` | 40, 19, 1 |
| R29 (em toda alteração) | Particionamento de nome, documento e papel (amostra); precedência do 400 de formato | `alteracaoComFormatoInvalidoDeFavorecidosERecusada`, `formatoDeFavorecidosNaAlteracaoVemAntesDoProcessoInexistente`, `formatoDeFavorecidosNaAlteracaoVemAntesDaSituacao`, `formatoDeFavorecidosNaAlteracaoVemAntesDoCredito` | 16, 4, 4, 4 |
| R15 e R29: recusa não cria nada e a cota continua livre | Tabela de decisão da cota (R6) combinada com a recusa | os mesmos testes de recusa acima (contagem de processos, crédito e abertura válida em seguida) | contado em cada teste |
| Precedência: 400 de formato de favorecidos antes de 404 e 409 | Suposição de erro | `favorecidosInvalidosVemAntesDeCotaInexistente`, `favorecidosInvalidosVemAntesDeCotaIndisponivel`, `favorecidosInvalidosVemAntesDeCreditoInsuficiente` | 4, 4, 4 |
| R15 e R29: consulta dos favorecidos (contrato) | Cobertura de terminais; efeito por outro endpoint | `favorecidosVoltamNaOrdemInformada`, `favorecidosDeProcessosDiferentesNaoSeConfundem`, `consultaDeFavorecidosEmProcessoInexistente`, `favorecidosPermanecemEmCadaSituacao` | 1, 1, 1, 15 |
| R16 (valor-limite: até 50.000,00, inclusive, aprovado; acima, aguardando alçada; nos dois tipos; no imobiliário a análise leva à vistoria) | Valor-limite e particionamento do valor; contrato do destino de concluir | `concluirDecidePelaAlcada` | 10 |
| R16 (a decisão é tomada ao concluir, com o valor vigente, depois de alterar em aberto ou de proposta com que o cliente concorda; proposta de que discorda não conta) | Tabela de decisão do valor vigente × resposta | `alteracaoEmAbertoMudaADecisaoDaAlcada`, `propostaSoMudaADecisaoDaAlcadaSeOClienteConcorda` | 8, 8 |
| R16 (devolver e concluir de novo decide de novo, com o valor vigente) | Sequência | `devolverEConcluirDeNovoSemMudarOValorDecideDeNovo`, `devolverEMudarOValorRefazADecisaoDaAlcada` | 2, 4 |
| R16 (aprovar na alçada leva a aprovado e segue até pago; cota indisponível e crédito não consumido em aguardando alçada) | Máquina de estados; efeito pela listagem, pela cota, pelo histórico e pelo crédito | `aprovarNaAlcadaSeguePeloPagamentoAtePago`, `aguardandoAlcadaMantemACotaIndisponivelESemConsumirCredito`, `listagemAcompanhaAAprovacaoNaAlcada`, `listagemFiltraPorSituacoesNovas`, `creditoDisponivelConsideraSoOsPagos` (cenários em aguardando alçada) | 2, 2, 1, 1 novo, 2 novos |
| R16 e R5 (aprovar na alçada fora de aguardando alçada é recusado; pedidos simultâneos, quem chega primeiro vale) | Máquina de estados: a coluna nova em toda situação; suposição de erro | `acaoForaDaOrdemERecusada`, `acaoEmProcessoInexistente`, `aprovarEReprovarNaAlcadaAoMesmoTempoValeUmSo`, `jsonMalformadoNaAcao`, `corpoQueNaoEJsonNaAcao`, `observacaoOpcionalAceitaObjetoVazio`, `observacaoFicaNoHistorico` | contado, 1 novo, 1, 1 novo, 1 novo, 4 novos, 8 novos |
| R18 (cada ação só de quem tem o papel; recusa de papel não muda nada) | Tabela de decisão papel × ação: 24 linhas × 7 credenciais; ordem das recusas; mais de um papel | `acaoSoEAceitaDeQuemTemOPapel`, `papelErradoVemAntesDoProcessoInexistente`, `papelErradoVemAntesDoFormatoDoCorpo`, `papelErradoVemAntesDaSituacaoEDosDadosDaAbertura`, `tokenSemPapelNaoExecutaAcaoDeEscrita`, `pessoaComDoisPapeisExerceOsDois`, `clienteComPapelDeBackofficeExerceOsDois` | 168, 1, 1, 1, 1, 1, 1 |
| R18 (só o cliente da cota altera, concorda e discorda; outro cliente é recusado) | Tabela papel × ação (cliente dono e outro cliente em cada linha); precedência do 404 | `acaoSoEAceitaDeQuemTemOPapel` (linhas de alterar, concordar e discordar), `donoEPeloClienteIdDoToken`, `processoInexistenteVemAntesDoClienteDeOutraCota` | 21, 2, 3 |
| R18 (consultar: todos os papéis) | Tabela de decisão consulta × papel | `consultaEDeTodosOsPapeis` | 32 |
| Decisão do coordenador: cadastros do backoffice | Tabela papel × cadastro | `cadastroSoEAceitoDoBackoffice` | 12 |
| Identidade: 401 | Particionamento do token (ausente, outra chave, expirado, outro emissor, malformado) × operação; ordem do 401 | `identificacaoAusenteOuInvalidaE401`, `identificacaoVemAntesDeTudo`, `openapiContinuaAberto` | 25, 5, 1 |
| R17 (quem aprova não pode ser quem concluiu a análise; recusa não muda nada; no imobiliário, quem concluiu a vistoria aprova) | Tabela de decisão (papel de aprovador × pessoa da última conclusão), nos dois tipos; sequência | `soAprovaQuemTemOPapelENaoConcluiuAAnalise`, `quemConcluiuAAnaliseNaoAprovaMasOutraPessoaDosMesmosPapeisAprova`, `quemSoIniciouAAnalisePodeAprovar`, `noImobiliarioQuemConcluiuSoAVistoriaPodeAprovar`, `noImobiliarioQuemConcluiuAAnaliseEAVistoriaNaoAprova` | 10, 2, 2, 1, 1 |
| R17 (a pessoa da última conclusão: depois de devolver e concluir de novo vale a nova) | Sequência | `depoisDeDevolverVaiValerAPessoaDaUltimaConclusao` | 2 |
| R19 (parte: motivo ou observação de cada ação aceita no histórico; recusada não entra) | Efeito observável pelo histórico: `de`, `para`, `acao`, `quando` em ordem, `observacao` | `fluxoChegaASituacaoFinal`, `motivoValidoFicaNoHistorico`, `observacaoFicaNoHistorico`, `acaoForaDaOrdemERecusada` (histórico não muda), `motivoAusenteERecusado`, `corpoAusenteEmAcaoComMotivo`, `motivoInvalidoERecusado` | contado em cada teste |
| R19 (valores anterior e novo nas alterações e propostas; nulos nas demais; recusada não entra) | Efeito observável pelo histórico | `fluxoChegaASituacaoFinal`, `fluxoComAlteracaoChegaASituacaoFinal`, `proporDiscordarProporConcordar`, `proporSemLimiteDeVezes`, `alterarVariasVezesEmAberto`, `recusaEmAguardandoConcordanciaPreservaAProposta`, `valorUmCentavoAcimaDoCreditoNaAlteracaoERecusado` | contado em cada teste |
| R19 (autor de cada ação aceita: o usuário de quem pediu, inclusive o cliente; "sistema" nas entradas da rotina; recusada não entra) | Máquina de estados: 14 caminhos completos com pessoas que se alternam; efeito pelo histórico | `autorDeCadaEntradaEQuemPediu`, `acaoSoEAceitaDeQuemTemOPapel` (autor da entrada nova de cada ação aceita), `tentativasEConfirmacaoDaRotinaTemAutorSistema`, `recusaPorPapelNaoEntraNoHistorico`, `pessoaComDoisPapeisExerceOsDois` | 14, 31, 1, 1, 1 |
| R19 (parte: tentativas e resultados da rotina no histórico, em ordem, com o motivo da recusa; tentativa descrita; recusada não entra) | Efeito observável pelo histórico | `fluxoChegaASituacaoFinal`, `respostaDaTesourariaTemOEfeitoDoModelo`, `indisponivelPorAlgumasTentativasDepoisConfirma`, `confirmacaoSemProtocoloNaoEAplicadaEDepoisConfirma`, `recusaSemMotivoNaoEAplicadaEDepoisRecusaComMotivo`, `recusaVoltaAAprovadoENovaSolicitacaoTemIdentificacaoNova` | contado em cada teste |
| R20 (a rotina pede com a identificação, confirma com protocolo e vai a pago; só ela tira o processo de pagamento solicitado; só olha pagamento solicitado) | Máquina de estados; tabela de decisão; efeito pela consulta, pelo histórico, pela cota e pelo que a tesouraria recebeu | `fluxoChegaASituacaoFinal`, `respostaDaTesourariaTemOEfeitoDoModelo`, `indisponivelPorAlgumasTentativasDepoisConfirma`, `rotinaSoOlhaProcessosEmPagamentoSolicitado`, `listagemAcompanhaARotina`, `listagemFiltraPorSituacoesNovas`, `cancelarEDevolverEmPagamentoSolicitadoSaoRecusados`, `solicitacoesSimultaneasDePagamentoValemUmaSo` | 61, 9, 2, 1, 1, 4, 2, 1 |
| R20 (valor e partes enviados são os vigentes; identificação não vazia; `processId`) | Cobertura de terminais do contrato da tesouraria; vigência | `pedidoTemOsDadosVigentesDepoisDeAlterarEmAberto`, `pedidoTemOsDadosVigentesDepoisDeConcordarComAProposta` | 1, 1 |
| R20, R13 (crédito só consumido em PAGO; pagamento solicitado e aprovado não consomem) | Tabela de decisão do crédito | `creditoSoDiminuiNoPagamento`, `pagamentoNaoConcluidoNaoConsomeCredito`, `creditoDisponivelConsideraSoOsPagos` (cenário em pagamento solicitado), `respostaDaTesourariaTemOEfeitoDoModelo` | 1, 1, 26, 9 |
| R6 (cota indisponível em pagamento solicitado; livre em pago; aprovado depois da recusa indisponível) | Tabela de decisão da cota | `cotaComProcessoEmAndamentoNaoAbreOutro` (inclui pagamento solicitado), `cotaComProcessoFinalizadoAbreOutro`, `respostaDaTesourariaTemOEfeitoDoModelo`, `recusaVoltaAAprovadoENovaSolicitacaoTemIdentificacaoNova` | 15 (fatia 6), 6, 9, 2 |
| R21 (recusa volta a aprovado, motivo no histórico; nova solicitação com identificação nova; sem limite de vezes) | Sequência; tabela de decisão | `recusaVoltaAAprovadoENovaSolicitacaoTemIdentificacaoNova`, `respostaDaTesourariaTemOEfeitoDoModelo` (recusou), `duasCotasEmPagamentoSolicitadoCadaUmaComSeuResultado` | 2, 2, 1 |
| R22 (mesma identificação nas novas tentativas; sem limite; cada tentativa no histórico; identificações diferentes entre processos) | Sequência; journal da tesouraria | `indisponivelPorAlgumasTentativasDepoisConfirma`, `semRespostaDepoisJaProcessouValeComoConfirmacao`, `respostaDaTesourariaTemOEfeitoDoModelo` (indisponível, demorou, incoerentes), `duasSolicitacoesPendentesTemIdentificacoesDiferentes`, `solicitacoesSimultaneasDePagamentoValemUmaSo` | 2, 1, 4, 1, 1 |
| R31 (confirmação sem protocolo e recusa sem motivo não são aplicadas; fato no histórico; nova tentativa; "já fiz, protocolo X" vale como confirmação) | Tabela de decisão; sequência | `respostaDaTesourariaTemOEfeitoDoModelo` (as duas incoerentes e já processou), `confirmacaoSemProtocoloNaoEAplicadaEDepoisConfirma`, `recusaSemMotivoNaoEAplicadaEDepoisRecusaComMotivo`, `semRespostaDepoisJaProcessouValeComoConfirmacao` | 3, 1, 1, 1 |
| R31 (protocolo diferente do informado antes para a mesma solicitação) | | não observável por fora (dúvida F5-1): sem teste | |
| R23 (com D18) | Máquina de estados: devolver de aprovado e de aguardando vistoria; particionamento do motivo | `fluxoChegaASituacaoFinal`, `acaoForaDaOrdemERecusada`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico` | contado em cada teste |
| R23 (devolver de aguardando alçada) | Máquina de estados; particionamento do motivo; efeito: volta a em análise, cota indisponível | `devolverEmAguardandoAlcadaVoltaParaAnalise`, `devolverEConcluirDeNovoSemMudarOValorDecideDeNovo`, `fluxoPelaAlcadaChegaASituacaoFinal`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico` | 2, 2, contado, 2 novos, contado |
| R4 (cancelar em aguardando alçada, com motivo; libera a cota) | Máquina de estados; particionamento do motivo | `reprovarOuCancelarEmAguardandoAlcadaEFinalELiberaACota`, `fluxoPelaAlcadaChegaASituacaoFinal`, `motivoAusenteERecusado`, `motivoValidoFicaNoHistorico` | 2, contado, 2 novos, contado |
| R6, R13, R14 (aguardando alçada está em andamento: cota indisponível, crédito não consumido; reprovado e cancelado a partir dela liberam a cota) | Tabela de decisão da cota; modelo de crédito | `cotaComProcessoEmAndamentoNaoAbreOutro` (inclui aguardando alçada), `aguardandoAlcadaMantemACotaIndisponivelESemConsumirCredito`, `reprovarOuCancelarEmAguardandoAlcadaEFinalELiberaACota` | 2 novos, 2, 4 |
| R19 (parte: `APROVAR_NA_ALCADA` e o destino de concluir no histórico, com `de` e `para`; recusada não entra) | Efeito observável pelo histórico | `concluirDecidePelaAlcada`, `aprovarNaAlcadaSeguePeloPagamentoAtePago`, `reprovarOuCancelarEmAguardandoAlcadaEFinalELiberaACota`, `devolverEmAguardandoAlcadaVoltaParaAnalise`, `aprovarEReprovarNaAlcadaAoMesmoTempoValeUmSo`, `fluxoPelaAlcadaChegaASituacaoFinal`, `acaoForaDaOrdemERecusada` (histórico não muda) | contado em cada teste |
| R18 (aprovar na alçada do aprovador; reprovar e devolver de aguardando alçada só do aprovador; cancelar do backoffice ou do aprovador) | Tabela papel × ação, linhas em aguardando alçada | `acaoSoEAceitaDeQuemTemOPapel` (reprovar, aprovar na alçada, devolver e cancelar em aguardando alçada) | 28 |
| R23 (devolver em pagamento solicitado é recusado) | Máquina de estados: ação inválida | `acaoForaDaOrdemERecusada`, `cancelarEDevolverEmPagamentoSolicitadoSaoRecusados` | contado, 2 |
| R24 (cliente altera em aberto; continua aberto; vale na hora; recusada deixa os anteriores) | Máquina de estados; particionamento; valor-limite | `alterarVariasVezesEmAberto`, `exemploDoCasoDeUsoDeAlteracaoEmAberto`, `alteracaoComFavorecidosValidosEAceita`, `fluxoComAlteracaoChegaASituacaoFinal`, `acaoForaDaOrdemERecusada` (alterar fora de aberto: 409) | 2, 1, 6, 6, contado |
| R25 (backoffice propõe em análise, com motivo; vai a aguardando concordância; vigentes não mudam; fora de análise, 409) | Máquina de estados; particionamento do motivo; vigência | `exemploDoCasoDeUsoDePropostaEmAnalise`, `propostaMostraOsNovosDadosEOsVigentesSeguemOsAnteriores`, `propostaPendenteNaoConsomeCredito`, `listagemAcompanhaARespostaDoCliente`, `listagemFiltraPorSituacoesNovas`, `favorecidosPermanecemEmCadaSituacao`, `creditoDisponivelConsideraSoOsPagos`, `motivoAusenteNaPropostaVemAntesDe404E409`, `motivoAusenteERecusado`, `motivoInvalidoERecusado`, `motivoValidoFicaNoHistorico` | 1, 2, 1, 1, 2, 2, 2, 1, 2, 6, 6 |
| R26 (concordar: a alteração vale, volta a em análise) | Máquina de estados; vigência | `proporDiscordarProporConcordar`, `proporSemLimiteDeVezes`, `alteracoesAteOPagamentoConsomemOCreditoDoValorFinal`, `observacaoOpcionalAceitaObjetoVazio`, `observacaoFicaNoHistorico`, `respostaRepetidaERecusada` | 2, 2, 4, 2, 4, 4 |
| R27 (discordar com motivo: desfaz, volta a em análise com os anteriores; propor de novo sem limite) | Máquina de estados; particionamento do motivo | `proporDiscordarProporConcordar`, `proporSemLimiteDeVezes`, `motivoAusenteNaDiscordanciaVemAntesDe404E409`, `motivoAusenteERecusado`, `motivoInvalidoERecusado`, `motivoValidoFicaNoHistorico` | 2, 2, 1, 2, 6, 6 |
| R28 (aguardando concordância: só concordar, discordar e cancelar; a proposta fica; cota indisponível) | Máquina de estados: cada ação inválida; tabela de decisão da cota | `recusaEmAguardandoConcordanciaPreservaAProposta`, `aguardandoConcordanciaAceitaSoTresAcoes`, `acoesAceitasNaSituacao`, `cotaComProcessoEmAndamentoNaoAbreOutro`, `concordarEDiscordarAoMesmoTempo` | 20, 2, 2, 2, 1 |
| Contrato: `alteracao-proposta` | Cobertura de terminais | `propostaDeProcessoInexistente`, `naoHaPropostaForaDeAguardandoConcordancia`, `propostaSomeDepoisDaResposta`, `propostaMostraOsNovosDadosEOsVigentesSeguemOsAnteriores` | 1, 15, 6, 2 |
| Contrato: ações novas e consulta de ações | Cobertura de terminais; suposição de erro | `acoesDeAlteracaoPorSituacao`, `jsonMalformadoNaAcao`, `corpoQueNaoEJsonNaAcao`, `corpoAusenteEmAcaoComMotivo`, `acaoEmProcessoInexistente` | 2, 4, 4, 2, 4 |
| R30 | implantação | sem teste de aceitação | |
| Contrato: consulta de ações | Cobertura de terminais; máquina de estados | `acoesAceitasNaSituacao` | 21 (fatia 6) |
| Contrato: listagem com `estado` novo | Cobertura de terminais | `listagemFiltraPorSituacoesNovas`, `listagemFiltraPorTipoESituacao`, `listagemPaginada` | 2, 1, 1 |
| Contrato: 400 por JSON malformado e 415 por tipo de conteúdo | Suposição de erro, em toda ação com corpo | `jsonMalformadoNaAcao`, `corpoQueNaoEJsonNaAcao` | 8, 8 |

### Dúvidas abertas da fatia 1 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| Ação fora da ordem com motivo ausente (por exemplo, reprovar um processo aprovado sem corpo): vale 409 ou 400? | Os casos de ação inválida sempre enviam motivo válido, então só a ordem é testada |
| Processo inexistente com motivo ausente: 404 ou 400? | `acaoEmProcessoInexistente` envia motivo válido |
| Corpo ausente em ação que exige motivo: `campos` traz `motivo`? | O teste confere 400 e `mensagem`, não `campos` |
| Observação vazia, só espaços ou acima de 500 caracteres: aceita ou recusada com 400? O contrato só lista o limite como "até 500" | Não testado: só observação curta e de 500 caracteres |
| Motivo com espaços nas pontas: o histórico guarda como veio ou aparado? | Não testado |
| `GET /processos/{id}/acoes` e `/historico` para processo inexistente: 404? | Não testado |
| `observacao` no histórico quando nenhuma foi informada: `null` ou ausente? | Não testado |
| Motivo ou observação com tipo errado (número, `null`) | Não testado; fica para a sessão exploratória |

### Dúvidas abertas da fatia 2 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| D2-1: valor acima do crédito disponível: o documento só conhece 409 (que também é "ação fora da ordem" e "cota em andamento"), sem 422. Usado 409 por ser o status de R6 na abertura; é o status certo? | Os testes esperam 409 e `mensagem` |
| D2-2: a mensagem de recusa por crédito informa quanto há disponível (seção Recusas); em que campo e formato? | Só se confere que `mensagem` existe |
| D2-3: valor com três casas mas zeros à direita (100.000), notação científica (1E+2) ou valor como texto ("100.00"): aceito ou 400? | Não testado |
| D2-4: valor acima do crédito e, ao mesmo tempo, cota com processo em andamento: qual recusa vale? | Não testado |
| D2-5: `GET /cotas/{id}` de cota inexistente: 404? `valorCredito` aparece na resposta? | Não testado; o crédito esperado é calculado pelo modelo a partir do crédito informado ao criar a cota |
| D2-6: valor acima de um crédito disponível, mas com número JSON enorme (overflow) | Não testado |
| D2-7: "Crédito disponível" com processo pago cujo valor foi implantado (R30) | Fora da aceitação (sem teste de R30) |

### Dúvidas abertas da fatia 3 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| F3-1: o documento (CPF ou CNPJ) tem dígito verificador conferido? Nesta entrega o contrato diz que não, mas R29 só diz "CPF ou CNPJ". Documento de 11 ou 14 dígitos com dígito verificador inválido deve ser aceito ou recusado? | Não testado; os documentos dos casos são sintéticos (sem dígito verificador válido) e são aceitos |
| F3-2: entre uma recusa de REGRA de favorecidos (soma diferente, documento repetido) e 404 (cota inexistente) ou 409 (cota com processo em andamento, crédito insuficiente), qual vem primeiro? A seção Recusas não dá ordem | Só a precedência do 400 de FORMATO é testada (`favorecidosInvalidosVemAntes...`). Regra × 404 e regra × 409 não são testadas |
| F3-3: favorecidos com tipo errado de JSON (`favorecidos` como objeto ou texto, elemento da lista nulo ou não objeto, `documento` como número JSON sem aspas, `nome` como número): 400 de formato? | Não testado |
| F3-4: R15 diz "o mesmo favorecido", sem dizer que a identidade é o documento. O contrato de quem coordena usa o documento. Duas pessoas com o mesmo nome e documentos diferentes são favorecidos diferentes? | Testado como aceito (`mesmo nome e papel com documentos diferentes`); documento repetido com nome ou papel diferentes testado como recusado |
| F3-5: a consulta devolve os favorecidos na ordem informada? O caso de uso (seção Consultas) só diz que o detalhe mostra "favorecidos e partes" | A ordem é do contrato de quem coordena e é conferida em todo caso aceito e em `favorecidosVoltamNaOrdemInformada` |
| F3-6: espaços nas pontas de `nome`, `papel` e `documento` (" Ana ", "12345678901 "): aparados, recusados ou guardados como vieram? | Não testado (só nome, papel e documento inteiramente em branco, recusados) |
| F3-7: pedido com mais de um defeito (por exemplo, parte zero e soma diferente): qual recusa vale, formato ou regra? E a lista vazia, em que a soma (zero) também difere do valor? | Cada caso tem um único defeito; a lista vazia é de formato pelo contrato |
| F3-8: a mensagem de recusa por soma informa "quanto falta ou sobra" (seção Recusas); em que campo e formato? A mensagem de documento repetido e a de dado faltando dizem qual favorecido? | Só se confere que `mensagem` existe (e `campos` nos de formato) |
| F3-9: "sem limite máximo" de favorecidos: há limite técnico (tamanho do corpo)? | Testado com 100 favorecidos; acima disso não |
| F3-10: parte com três casas e zeros à direita (40.000), notação científica (4E+1) ou valor como texto ("40.00"): aceita ou 400? (mesma dúvida de D2-3 para o valor do processo) | Não testado |
| F3-11: nome e papel com mais de uma unidade de caracteres por símbolo (emoji): como se conta o tamanho? | Só caracteres do plano básico são testados (inclusive acentuados) |
| F3-12: a seção Recusas lista "sem favorecido" em duas linhas (favorecidos, e abrir para cota inexistente, tipo inválido, sem valor ou sem favorecido), com status não definido; o contrato da fatia usa 400 | Não testado além do 400 do contrato |
| F3-13: R30 dá a cada processo existente um favorecido com o nome do cliente da cota; o que `GET /processos/{id}/favorecidos` devolve para processo anterior à implantação, e qual `documento` ele leva (a cota só informa o CPF do cliente)? | Fora da aceitação (R30 sem teste) |

### Dúvidas abertas da fatia 4 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| F4-1: entre uma recusa de REGRA (soma diferente do valor, documento repetido) em `alterar` ou `propor-alteracao` e 404 ou 409 (situação, crédito), qual vem primeiro? | Só a precedência do 400 de FORMATO é testada. Regra × 404 e regra × 409 não são testadas |
| F4-2: com a ação fora da situação (ex.: `concordar` em análise) e motivo ausente em `discordar`, vale 400 ou 409? O contrato desta fatia manda 400 antes de 404 e 409 para os campos das ações novas; para as ações antigas continua a dúvida da fatia 1 | Testado só para `propor-alteracao` e `discordar` (`motivoAusenteNa...VemAntesDe404E409`) |
| F4-3: `GET /processos/{id}/alteracao-proposta` traz `mensagem` no 404 (sem proposta pendente, processo inexistente)? R9 fala de recusas; consulta é outra coisa | Só o status 404 é conferido |
| F4-4: alterar ou propor com exatamente os mesmos valor e favorecidos que já valem: aceito (e entra no histórico) ou recusado? | Não testado |
| F4-5: `ALTERAR` no histórico: `de` e `para` são ABERTO e ABERTO? E `valorAnterior` e `valorNovo` nulos ou ausentes nas outras ações (ausente também passa)? | Testado `de` = `para` = ABERTO; nulo ou ausente aceitos |
| F4-6: um favorecido da proposta pode repetir o documento de um favorecido vigente (a regra de repetição é dentro do conjunto novo)? | Não testado |
| F4-7: pedidos simultâneos de alterar e propor, ou de concordar e cancelar, sobre o mesmo processo | Só concordar e discordar simultâneos são testados |
| F4-8: R16 (alçada decidida depois de qualquer alteração) e R13 conferido de novo ao concordar (o crédito pode ter mudado entre propor e concordar, D21) | Fora desta fatia (alçada: fatia 6; D21 aberta) |
| F4-9: o motivo da proposta aparece também em `observacao` do histórico e na consulta da proposta; quem o vê, e em que canal, é da fatia 7 | Conferidos o histórico e a consulta, sem papel |
| F4-10: papéis (só o cliente da cota altera, concorda e discorda; só o backoffice propõe; cliente não cancela) | Coberto na fatia 7 (R18): `acaoSoEAceitaDeQuemTemOPapel`, `donoEPeloClienteIdDoToken` |
| F4-11: `alterar` e `propor-alteracao` com `valor` ou parte em número com zeros à direita, notação científica ou texto (mesma dúvida de D2-3 e F3-10) | Não testado |

### Dúvidas abertas da fatia 5 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| F5-1: protocolo diferente do informado antes para a mesma solicitação (R31). A tesouraria só informa o protocolo quando confirma ou diz que já fez: depois do primeiro, o segundo diferente não tem como ser observado por fora (o processo já está em PAGO e a rotina não pergunta mais) | Não testado; registrado como não observável |
| F5-2: tentativa em que o circuito de proteção está aberto e a tesouraria nem é chamada: entra no histórico como `TENTAR_PAGAMENTO`? O caso de uso diz "cada tentativa vai ao histórico" | Os testes esperam "ao menos N" entradas e, quando precisam de N pedidos recebidos pela tesouraria, esperam por eles à parte; nunca comparam um número com o outro. Observado: com a tesouraria fora do ar (503), o circuito deixa passar um único pedido de prova a cada reabertura, sempre do primeiro processo da rodada; com mais de um processo pendente, nem toda tentativa chega a ela. Por isso os casos com dois ou mais processos pendentes usam resposta incoerente (R31), e não erro, quando contam pedidos recebidos |
| F5-3: protocolo ou motivo em branco (só espaços, vazio) na resposta da tesouraria é incoerente como o ausente? E `ALREADY_PROCESSED` sem protocolo? E status desconhecido, corpo que não é JSON, 4xx ou 200 sem corpo? O caso de uso só lista "confirmação sem protocolo" e "recusa sem motivo" | Não testado: só as duas incoerências do caso de uso, com o campo ausente |
| F5-4: "sem resposta" é a demora acima de 2 segundos (contrato de quem coordena, não do caso de uso). O limite exato não é testado | Testados 1 segundo (aplicada) e 3 segundos (sem resposta) |
| F5-5: o que a pessoa informa em `observacao` ao solicitar pagamento vai ao histórico (como nas demais ações com observação) e o limite é de 500 caracteres? (mesma dúvida da fatia 1 para observação vazia, só espaços ou acima de 500) | Testadas só observação curta e de 500 caracteres |
| F5-6: o detalhe do processo mostra "o motivo da última recusa da tesouraria" (seção Consultas); o contrato não tem consulta de detalhe, e o motivo é conferido no histórico (`RECUSAR_PAGAMENTO`) | Só o histórico é conferido |
| F5-7: depois da recusa da tesouraria, o motivo continua visível quando o processo é solicitado de novo e confirmado? | Não testado |
| F5-8: o intervalo padrão de 5 minutos (R20) e a configuração do intervalo não são testados: o ambiente de teste fixa 1 segundo | Sem teste do valor padrão |
| F5-9: duas execuções da rotina ao mesmo tempo sobre o mesmo processo (seção Recusas): "um único pedido efetivo à tesouraria e uma única mudança de situação". Sem endpoint para disparar a rotina, a concorrência não é provocável por fora | Testado o equivalente observável: solicitações de pessoa simultâneas (uma vale, uma só identificação chega à tesouraria) |
| F5-10: R7 diz que nada é enviado à tesouraria ao solicitar. Entre solicitar e a primeira rodada da rotina não dá para provar a ausência de pedido | Prova-se só que solicitar responde 200 com a tesouraria fora do ar e que a rotina não pede por processos fora de pagamento solicitado |
| F5-11: quando a rotina aplica a recusa e a tesouraria já tinha recebido a ordem de uma tentativa anterior sem resposta (dois pedidos, uma só identificação), há algo a fazer além de aplicar a resposta? | Não testado |
| F5-12: o autor das entradas da rotina ("sistema") e o do pedido de solicitar (papéis) | Coberto na fatia 7 (R19): `autorDeCadaEntradaEQuemPediu`, `tentativasEConfirmacaoDaRotinaTemAutorSistema` |

### Dúvidas abertas da fatia 6 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| F6-1: R16 e D35 dizem que a alçada é decidida ao concluir, "depois de qualquer alteração de valor (que só é possível antes dele)". Mas R23 permite devolver para análise a partir de aprovado e de aguardando alçada, e em análise o backoffice pode propor outra alteração (R25): a alteração acontece depois de uma decisão anterior. Ao concluir de novo, a alçada é decidida de novo, com o valor vigente daquele momento? E se o valor passou a ser menor que a alçada depois de o processo já ter estado em aguardando alçada, ou o contrário, nada do que foi decidido antes vale? | Testado como decidido por quem coordena: a decisão se refaz a cada conclusão, com o valor vigente (`devolverEMudarOValorRefazADecisaoDaAlcada`, `devolverEConcluirDeNovoSemMudarOValorDecideDeNovo`). Falta o dono confirmar |
| F6-2: a ação nova se chama `aprovar-na-alcada` e entra no histórico como `APROVAR_NA_ALCADA`; em `CONCLUIR_ANALISE` e `CONCLUIR_VISTORIA` o campo `para` traz APROVADO ou AGUARDANDO_ALCADA. O caso de uso só diz que cada ação aceita vai ao histórico (R19) | Nomes e `para` do contrato de quem coordena, conferidos como tal; não são regra do dono |
| F6-3: `aprovar-na-alcada` com a observação vazia, só espaços ou acima de 500 caracteres (mesma dúvida da fatia 1 para a observação) | Testadas só observação curta e de 500 caracteres |
| F6-4: pedido de reprovar, devolver ou cancelar em aguardando alçada com motivo ausente, fora da ordem (por exemplo, o mesmo pedido depois de o processo já ter sido aprovado na alçada): 400 ou 409? (mesma dúvida da fatia 1) | Os casos de ação inválida enviam motivo válido |
| F6-5: R13 conferido de novo ao concluir, ao aprovar na alçada ou ao solicitar pagamento, se o crédito da cota mudou (D21, aberta). Um processo acima da alçada espera uma segunda pessoa e pode ficar aguardando por muito tempo; sem prazo nem aviso (fora do escopo) | Não testado; o crédito disponível é conferido sem mudar o cadastro da cota |
| F6-6: o valor da alçada é configurável (D1) e a configuração não é conferida: os testes usam o padrão de R$ 50.000,00 e valores de R$ 49.999,99 a R$ 50.000,01 | Sem teste da configuração (como F5-8) |
| F6-7: R17 (quem aprova não pode ser quem concluiu a análise; no imobiliário, quem concluiu a vistoria pode aprovar) e R18 (reprovar e devolver de aguardando alçada só do aprovador; cancelar do backoffice ou do aprovador; aprovar na alçada do aprovador) | Coberto na fatia 7: `acaoSoEAceitaDeQuemTemOPapel`, `soAprovaQuemTemOPapelENaoConcluiuAAnalise` e os demais testes de R17 |
| F6-8: quem concluiu a análise, para R17, é "a pessoa da última conclusão" (D16). Depois de devolver de aguardando alçada e concluir de novo, vale a nova conclusão; no imobiliário a análise é concluída por uma pessoa e a vistoria por outra | Coberto na fatia 7: `depoisDeDevolverVaiValerAPessoaDaUltimaConclusao`, `noImobiliarioQuemConcluiuSoAVistoriaPodeAprovar` |
| F6-9: aprovar na alçada e reprovar ao mesmo tempo: o caso de uso manda tratar o perdedor como ação fora da ordem (seção Recusas); o status do perdedor é o 409 do contrato | Testado: um 200 e os demais 409, com histórico e situação coerentes com a vencedora |
| F6-10: o detalhe do processo mostra o valor vigente e a situação; o caso de uso não diz se em aguardando alçada a consulta mostra também a decisão pendente (a pessoa que concluiu a análise, quando) | Fora do contrato desta fatia; o autor já está no histórico (fatia 7), mas o detalhe com a decisão pendente continua fora do contrato |
| Inconsistência de contagem: a Rastreabilidade registrava 833 casos na fatia 5; recontados antes da fatia 6 eram 864 | Corrigido na Rastreabilidade; sem efeito sobre as regras |

### Dúvidas abertas da fatia 7 (nenhuma virou asserção)

| Dúvida | Efeito no teste |
|---|---|
| F7-1: o coordenador manda conferir o papel antes de olhar o processo, mas reprovar e devolver para análise mudam de papel conforme a situação (backoffice em análise, aprovador em aguardando alçada). Se o papel é conferido antes do processo, o backoffice reprovar em aguardando alçada (papel certo para outra situação) é 403 ou 409? E o aprovador reprovar um processo aprovado? | Só é testada a recusa de papel com o processo na situação em que a ação vale. Os casos de ação fora da ordem (409) usam, nos auxiliares, uma pessoa com os dois papéis ("carla") |
| F7-2: ordem entre o 403 de cliente de outra cota (e o de R17) e o 409 (ação fora da ordem, cota em andamento); e entre o 403 de outro cliente e o 400 de formato do corpo na abertura (a cota só é conhecida lendo o corpo) | Não testado. O 403 de cota só é conferido com o processo na situação em que a ação vale e o corpo válido |
| F7-3: o cliente de outra cota pode consultar o histórico, a proposta, os favorecidos e as ações de um processo que não é dele? O quadro diz "consultar: todos os papéis", e D37 diz que o cliente da cota consulta a proposta e o histórico | Não testado para o cliente de outra cota nas consultas de detalhe do processo; testado só na lista de processos e na consulta da cota |
| F7-4: quem concluiu a análise pode reprovar, devolver ou cancelar em aguardando alçada? R17 só proíbe aprovar | Não testado; as recusas de R17 são só de aprovar |
| F7-5: token com papel `cliente` e sem `clienteId`; token com grupo desconhecido; usuário sem nenhum papel em consulta e em cadastro de consulta; `clienteId` de cliente que não existe | Testado só: usuário sem nenhum papel não escreve (403). O resto não é testado |
| F7-6: os cadastros `/clientes` e `/cotas` são do backoffice por decisão do coordenador, o caso de uso não os trata. Os códigos de sucesso são só 2xx; o 403 não prova que nada foi criado (não há como identificar o que seria criado); `GET /clientes` não tem contrato nos testes | Cadastro conferido por status; sem consulta de efeito na recusa |
| F7-7: os canais `/web`, `/mobile`, `/taa` (qualquer papel autenticado, 401 sem token) e `/q/health` (aberto) não têm caminhos nem respostas conhecidos pelos testes de aceitação | Não testados. Só `/q/openapi` aberto é conferido |
| F7-8: o corpo de 401 e de 403: o formato de erro de sempre (`mensagem`), ou o corpo padrão do framework (vazio)? | Conferidos o status e, se o corpo vier preenchido, a `mensagem` |
| F7-9: tolerância de relógio na expiração do token e emissor com barra final ou diferença de caixa | Testado só token expirado há uma hora e emissor claramente outro |
| F7-10: a regra da segunda pessoa compara usuários (`sub`). Uma mesma pessoa com duas contas, ou o cliente que também é backoffice propor e concordar a própria alteração, fica fora do que o caso de uso responde | Não testado (o cliente com papel de backoffice só abre e inicia a análise) |
| F7-11: `POST /processos/{id}/pagar` (endpoint que deixou de existir) com token de outro papel, ou sem token: 401, 403 ou 404? | O auxiliar o chama com o token da tesouraria e espera 404, como na fatia 5. Os outros não são testados |
| F7-12: processos que já existiam antes da implantação (R30) e as entradas de histórico antigas não têm autor | Fora da aceitação (sem teste de R30) |
| Inconsistência do caso de uso: Atores diz que o cliente "consulta a alteração proposta e o histórico" e o quadro diz "consultar: todos os papéis"; R4 e o quadro dizem que cancelar é do backoffice e do aprovador, mas o quadro de ciclo de vida e R11 não dizem qual papel reprova em análise (só o quadro de Atores: backoffice) | Registrada, sem resolver (F7-3 e F7-1) |
