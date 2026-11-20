# language: pt
Funcionalidade: Abertura do processo de pagamento (R1)
  Um processo é aberto para uma cota, com o tipo e o valor a pagar.
  Ele começa em "Aberto", e só pode ser aberto com valor maior que zero.

  Como preencher: uma linha por cenário. "resultado" é aceita ou recusada; "número" e "situação"
  vazios não são conferidos.

  Esquema do Cenário: <cenário> <descrição>
    Quando alguém abre um processo de <tipo>, da cota "<cota>", com valor de <valor>
    Então a abertura é "<resultado>", com número "<número>" e situação "<situação>"

    Exemplos:
      | cenário | regra | descrição                                                   | tipo    | cota     | valor     | resultado | número | situação |
      | 1.1     | R1    | Processo aberto recebe o primeiro número livre, em "Aberto" | serviço | COTA-100 | 30.000,00 | aceita    | 1000   | Aberto   |
      | 1.2     | R1    | Processo sem valor não é aberto                             | serviço | COTA-100 | 0,00      | recusada  |        |          |
