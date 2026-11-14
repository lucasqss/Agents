-- Autoria e papéis (fatia 7, ADR 003).
--
-- Histórico (R19): cada linha passa a guardar quem a fez, o usuário do token ou 'sistema' na rotina de pagamento.
-- As linhas que já existem foram gravadas antes de haver identidade: o autor delas é desconhecido de verdade, e
-- 'desconhecido' diz isso sem inventar uma pessoa nem fingir que foi a rotina. Depois, a coluna deixa de aceitar nulo.
alter table processo_transicao add column autor varchar(100);
update processo_transicao set autor = 'desconhecido';
alter table processo_transicao alter column autor set not null;

-- R17: quem aprova na alçada não pode ser quem concluiu a análise pela última vez. A raiz guarda esse usuário no
-- processo, em vez de reler o histórico. Nulo em processo que nunca teve a análise concluída e nos que já existiam:
-- o histórico antigo não diz quem concluiu, então a regra só vale a partir da próxima conclusão de análise.
alter table processo_pagamento add column autor_ultima_conclusao_analise varchar(100);
