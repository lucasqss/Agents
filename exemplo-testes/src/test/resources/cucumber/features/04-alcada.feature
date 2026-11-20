# language: pt
@R4
Funcionalidade: Alçada de aprovação
  Até a alçada (R$ 50.000,00), inclusive, o processo é aprovado direto.
  Acima dela, aguarda a aprovação de quem tem alçada.

  Esquema do Cenário: <cenário> Concluir a análise de um serviço de <valor> leva a "<situação>"
    Dado o processo <processo> da massa, de serviço, na situação "Em análise", com valor de <valor>
    Quando "ana" concluir a análise
    Então o processo fica "<situação>"

    Exemplos:
      | cenário | processo | valor     | situação          |
      | 4.1     | 5        | 50.000,00 | Aprovado          |
      | 4.2     | 6        | 50.000,01 | Aguardando alçada |

  Cenário: 4.3 Imobiliário acima da alçada aguarda a alçada depois da vistoria e o aprovador libera
    Dado o processo 4 da massa, de imobiliário, na situação "Aguardando vistoria", com valor de 80.000,00
    Quando "vitor" concluir a vistoria
    Então o processo fica "Aguardando alçada"
    E as ações disponíveis são:
      | aprovar na alçada     |
      | devolver para análise |
      | reprovar              |
      | cancelar              |
    Quando "carla" aprovar na alçada
    Então o processo fica "Aprovado"
