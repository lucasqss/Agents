# language: pt
Funcionalidade: Ações sobre o processo (R2 a R7)
  O processo anda pelas ações que o tipo dele permite em cada situação: o imobiliário passa pela vistoria,
  acima da alçada (R$ 50.000,00) o processo aguarda quem tem alçada, algumas ações exigem motivo,
  situações finais não aceitam nada e o resto pode ser cancelado.

  Como preencher: uma linha por cenário, sobre um processo da massa (massa/processos.md), citado pela cota.
  Cada cota é usada por um cenário só. "resultado" é aceita ou recusada; "motivo" vazio manda a ação sem motivo;
  "mensagem" e "ações disponíveis" vazias não são conferidas; "ações disponíveis" separadas por vírgula, ou nenhuma.

  Esquema do Cenário: <cenário> <descrição>
    Dado o processo da cota "<cota>" da massa
    Quando "<quem>" pedir para "<ação>" com o motivo "<motivo>"
    Então a ação é "<resultado>" e o processo fica "<situação>"
    E a mensagem é "<mensagem>"
    E as ações disponíveis são "<ações disponíveis>"

    Exemplos:
      | cenário | regra | descrição                                               | cota     | quem  | ação                | motivo           | resultado | situação          | mensagem                                          | ações disponíveis                                           |
      | 2.2     | R2    | Processo aberto não pode ser pago sem passar pela análise | COTA-011 | ana   | pagar               |                  | recusada  | Aberto            | Não é possível pagar um processo no estado ABERTO |                                                             |
      | 3.1     | R3    | Imobiliário dentro da alçada é aprovado na vistoria     | COTA-003 | vitor | concluir a vistoria |                  | aceita    | Aprovado          |                                                   |                                                             |
      | 3.2     | R3    | Serviço não tem vistoria                                | COTA-010 | vitor | concluir a vistoria |                  | recusada  | Em análise        |                                                   |                                                             |
      | 4.1     | R4    | Valor igual à alçada é aprovado direto                  | COTA-005 | ana   | concluir a análise  |                  | aceita    | Aprovado          |                                                   |                                                             |
      | 4.2     | R4    | Valor um centavo acima da alçada aguarda a alçada       | COTA-006 | ana   | concluir a análise  |                  | aceita    | Aguardando alçada |                                                   | aprovar na alçada, devolver para análise, reprovar, cancelar |
      | 6.1     | R6    | Processo pago não aceita nenhuma ação, nem cancelar     | COTA-007 | ana   | cancelar            | desistência      | recusada  | Pago              |                                                   | nenhuma                                                     |
      | 7.1     | R7    | Processo à espera de documento pode ser cancelado       | COTA-008 | ana   | cancelar            | cliente desistiu | aceita    | Cancelado         |                                                   | nenhuma                                                     |
