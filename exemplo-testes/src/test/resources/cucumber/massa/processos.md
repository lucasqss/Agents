# Massa da aceitação: processos de pagamento

Os processos que já existem quando os cenários começam, cada um numa situação de que algum cenário precisa.
O cenário cita o processo pela **cota** (a chave), nunca por um número.

Como preencher:

- **cota:** única, até 30 caracteres. `COTA-SENTINELA` é reservada (o carregador a insere sozinho, para provar que a
  aplicação subiu sobre este banco).
- **tipo:** serviço ou imobiliário.
- **valor:** em reais, como `50.000,01`.
- **situação:** Aberto, Em análise, Pendente de documento, Aguardando vistoria, Aguardando alçada, Aprovado, Pago,
  Reprovado ou Cancelado.
- **observação:** livre, para quem lê. Não vai para o banco.
- **Uma cota por cenário:** dois cenários sobre o mesmo processo dependeriam da ordem. O build barra.
- **Linha nova:** em qualquer posição. Os cenários citam a cota, então nada muda nos outros.

Um termo fora da lista, uma cota repetida ou um valor mal escrito param o build antes de a aplicação subir, dizendo a
linha.

| cota     | tipo        | valor     | situação              | observação                         |
|----------|-------------|-----------|-----------------------|------------------------------------|
| COTA-002 | serviço     | 30.000,00 | Em análise            | reprovar sem e com motivo (5.1)    |
| COTA-003 | imobiliário | 30.000,00 | Aguardando vistoria   | dentro da alçada (3.1)             |
| COTA-004 | imobiliário | 80.000,00 | Aguardando vistoria   | acima da alçada (4.3)              |
| COTA-005 | serviço     | 50.000,00 | Em análise            | exatamente na alçada (4.1)         |
| COTA-006 | serviço     | 50.000,01 | Em análise            | um centavo acima da alçada (4.2)   |
| COTA-007 | serviço     | 30.000,00 | Pago                  | situação final (6.1)               |
| COTA-008 | serviço     | 30.000,00 | Pendente de documento | cancelar com motivo (7.1)          |
| COTA-009 | serviço     | 80.000,00 | Aguardando alçada     | histórico de duas ações (8.1)      |
| COTA-010 | serviço     | 30.000,00 | Em análise            | serviço não tem vistoria (3.2)     |
| COTA-011 | serviço     | 30.000,00 | Aberto                | pagar sem análise (2.2)            |
