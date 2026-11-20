# language: pt
@R8
Funcionalidade: Histórico do processo
  Cada mudança de situação fica registrada, com quem fez e o motivo, quando houver.

  Cenário: 8.1 O histórico guarda cada transição, com quem fez
    Dado o processo 9 da massa, de serviço, na situação "Aguardando alçada", com valor de 80.000,00
    Quando "carla" aprovar na alçada
    Então o processo fica "Aprovado"
    Quando "tesouraria" pagar
    Então o processo fica "Pago"
    E o histórico tem 2 registros
    E o último registro do histórico é:
      | ação | pagar      |
      | quem | tesouraria |
      | de   | Aprovado   |
