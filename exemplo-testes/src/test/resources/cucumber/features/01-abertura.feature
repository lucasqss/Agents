# language: pt
@R1
Funcionalidade: Abertura do processo de pagamento
  Um processo é aberto para uma cota, com o tipo e o valor a pagar.
  Ele começa em "Aberto", e só pode ser aberto com valor maior que zero.

  Cenário: 1.1 Processo aberto recebe o primeiro número livre e começa em "Aberto"
    Quando alguém abre um processo de serviço, da cota "COTA-100", com valor de 30.000,00
    Então o processo é aberto com o número 1000
    E o processo continua "Aberto"
    E as ações disponíveis são:
      | iniciar a análise |
      | cancelar          |

  Cenário: 1.2 Processo sem valor não é aberto
    Quando alguém abre um processo de serviço, da cota "COTA-100", com valor de 0,00
    Então a abertura é recusada
