-- Valor do processo (R12). Implantação (R30): os processos existentes recebem o crédito da cota.
alter table processo_pagamento add column valor numeric(15, 2);
update processo_pagamento set valor = (select c.valor_credito from cota c where c.id = processo_pagamento.cota_id);
alter table processo_pagamento alter column valor set not null;
