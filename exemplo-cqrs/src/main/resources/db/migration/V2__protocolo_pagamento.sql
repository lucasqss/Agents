-- Protocolo devolvido pela tesouraria (sistema externo) quando o processo é pago.
alter table processo_pagamento add column protocolo_pagamento varchar(40);
