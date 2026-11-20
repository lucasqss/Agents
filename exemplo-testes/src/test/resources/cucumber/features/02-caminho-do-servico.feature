# language: pt
@R2
Funcionalidade: Caminho do processo de serviço
  O processo anda pelas ações que o tipo dele permite em cada situação.
  Uma ação que não vale na situação atual é recusada e nada muda.

  Cenário: 2.1 Serviço vai de "Aberto" a "Pago" pelo caminho comum
    Dado um processo novo de serviço, da cota "COTA-101", com valor de 30.000,00
    Então o processo é aberto com o número 1001
    Quando "ana" iniciar a análise
    Então o processo fica "Em análise"
    Quando "ana" concluir a análise
    Então o processo fica "Aprovado"
    Quando "tesouraria" pagar
    Então o processo fica "Pago"
    E nenhuma ação está disponível
    E o histórico tem 3 registros
    E o último registro do histórico é:
      | ação | pagar      |
      | quem | tesouraria |
      | de   | Aprovado   |
      | para | Pago       |

  Cenário: 2.2 Processo aberto não pode ser pago sem passar pela análise
    Dado o processo 11 da massa, de serviço, na situação "Aberto"
    Quando "ana" pagar
    Então a ação é recusada com a mensagem "Não é possível pagar um processo no estado ABERTO"
    E o processo continua "Aberto"
