-- Controle de concorrência otimista: cada gravação confere e incrementa a versão.
alter table processo_pagamento add column versao bigint not null default 0;
