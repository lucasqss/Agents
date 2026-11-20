-- Massa da aceitação (só teste): roda depois de todas as migrations V*, num banco novo a cada execução (BancoDaAceitacao).
-- Um processo pronto em cada situação de que um cenário precisa, com ID fixo. Regras:
--   * ID da massa fica abaixo de 1000; cada processo é usado por UM cenário (Cenarios.validar confere).
--   * Registro novo: uma linha nova aqui, com o próximo ID livre. Não reaproveite o de outro cenário.
--   * A massa entra sem histórico: o histórico conferido nos cenários é só o que eles mesmos fizeram.
--   * O processo 1 é a SENTINELA: nenhum cenário o usa (Cenarios.validar barra), e o processo.feature confere, antes dos
--     cenários, que ele está aqui como foi gravado. Prova que a aplicação subiu sobre este banco, e não sobre outro.
insert into processo_pagamento (id, tipo, numero_cota, valor, estado) values
    ( 1, 'SERVICO',     'COTA-SENTINELA', 30000.00, 'ABERTO'),
    ( 2, 'SERVICO',     'COTA-002', 30000.00, 'EM_ANALISE'),
    ( 3, 'IMOBILIARIO', 'COTA-003', 30000.00, 'AGUARDANDO_VISTORIA'),
    ( 4, 'IMOBILIARIO', 'COTA-004', 80000.00, 'AGUARDANDO_VISTORIA'),
    ( 5, 'SERVICO',     'COTA-005', 50000.00, 'EM_ANALISE'),     -- exatamente na alçada
    ( 6, 'SERVICO',     'COTA-006', 50000.01, 'EM_ANALISE'),     -- um centavo acima da alçada
    ( 7, 'SERVICO',     'COTA-007', 30000.00, 'PAGO'),
    ( 8, 'SERVICO',     'COTA-008', 30000.00, 'PENDENTE_DOCUMENTO'),
    ( 9, 'SERVICO',     'COTA-009', 80000.00, 'AGUARDANDO_ALCADA'),
    (10, 'SERVICO',     'COTA-010', 30000.00, 'EM_ANALISE'),
    (11, 'SERVICO',     'COTA-011', 30000.00, 'ABERTO');

-- Tudo o que os cenários criarem começa em 1000 (1000, 1001...), na ordem em que os cenários rodam.
alter table processo_pagamento alter column id restart with 1000;
alter table processo_transicao alter column id restart with 1000;
