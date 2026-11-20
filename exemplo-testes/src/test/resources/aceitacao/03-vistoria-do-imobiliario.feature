# language: pt
@R3
Funcionalidade: Vistoria do processo imobiliário
  O processo imobiliário passa pela vistoria depois da análise.
  O processo de serviço não tem vistoria.

  Cenário: 3.1 Imobiliário dentro da alçada é aprovado ao concluir a vistoria
    Dado o processo 3 da massa, de imobiliário, na situação "Aguardando vistoria", com valor de 30.000,00
    Quando "vitor" concluir a vistoria
    Então o processo fica "Aprovado"

  Cenário: 3.2 Serviço não tem vistoria
    Dado o processo 10 da massa, de serviço, na situação "Em análise"
    Quando "vitor" concluir a vistoria
    Então a ação é recusada
    E o processo continua "Em análise"
