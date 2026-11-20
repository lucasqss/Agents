# language: pt
Funcionalidade: Encerramento do processo
  "Pago", "Reprovado" e "Cancelado" são situações finais: não aceitam nenhuma ação.
  Em qualquer outra situação o processo pode ser cancelado, com motivo.

  @R6
  Cenário: 6.1 Processo pago não aceita nenhuma ação, nem cancelar
    Dado o processo 7 da massa, de serviço, na situação "Pago"
    Quando "ana" cancelar com o motivo "desistência"
    Então a ação é recusada
    E o processo continua "Pago"
    E nenhuma ação está disponível

  @R7
  Cenário: 7.1 Processo à espera de documento pode ser cancelado, com motivo
    Dado o processo 8 da massa, de serviço, na situação "Pendente de documento"
    Quando "ana" cancelar com o motivo "cliente desistiu"
    Então o processo fica "Cancelado"
    E nenhuma ação está disponível
