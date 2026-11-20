# language: pt
@R5
Funcionalidade: Motivo obrigatório
  Solicitar documento, devolver para análise, reprovar e cancelar exigem um motivo,
  que fica registrado no histórico.

  Cenário: 5.1 Reprovar sem motivo é recusado; com motivo, o motivo fica no histórico
    Dado o processo 2 da massa, de serviço, na situação "Em análise"
    Quando "ana" reprovar
    Então a ação é recusada com a mensagem "Motivo é obrigatório para reprovar um processo"
    E o processo continua "Em análise"
    Quando "ana" reprovar com o motivo "renda incompatível"
    Então o processo fica "Reprovado"
    E o histórico tem 1 registro
    E o último registro do histórico é:
      | ação   | reprovar           |
      | quem   | ana                |
      | motivo | renda incompatível |
