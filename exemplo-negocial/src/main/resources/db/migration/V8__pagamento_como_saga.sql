-- Pagamento como saga: o processo espera em PAGAMENTO_SOLICITADO até a rotina falar com a tesouraria.
-- Identificação da solicitação de pagamento: a mesma em todas as tentativas da rotina (a tesouraria não abre duas
-- ordens) e nova a cada solicitação. Só existe enquanto o processo está em PAGAMENTO_SOLICITADO.
-- Nenhum processo existente está nesse estado (ele é novo): as linhas atuais ficam com a coluna nula, inclusive os
-- processos pagos antes desta migration, que continuam válidos com o protocolo que já guardam.
alter table processo_pagamento add column identificacao_solicitacao uuid;

alter table processo_pagamento add constraint ck_processo_identificacao_solicitacao
    check ((estado = 'PAGAMENTO_SOLICITADO') = (identificacao_solicitacao is not null));

-- A rotina lista só os processos à espera da tesouraria.
create index ix_processo_pagamento_solicitado on processo_pagamento (id) where estado = 'PAGAMENTO_SOLICITADO';
