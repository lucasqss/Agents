# language: pt
Funcionalidade: Fluxos de vários passos (R2, R4, R5, R8)
  Cenários em que o processo passa por várias ações seguidas, e o histórico guarda cada uma,
  com quem fez e o motivo, quando houver.

  Como preencher: um cenário por fluxo; os passos em tabela, um por linha, na ordem. "resultado" é aceita
  ou recusada; "motivo" vazio manda a ação sem motivo; "mensagem" vazia não é conferida.

  Cenário: 2.1 Serviço vai de "Aberto" a "Pago" pelo caminho comum
    Dado um processo novo de serviço, da cota "COTA-101", com valor de 30.000,00
    Então o processo é aberto com o número 1001
    Quando os passos abaixo são executados em ordem:
      | quem       | ação               | motivo | resultado | situação   |
      | ana        | iniciar a análise  |        | aceita    | Em análise |
      | ana        | concluir a análise |        | aceita    | Aprovado   |
      | tesouraria | pagar              |        | aceita    | Pago       |
    Então o histórico tem 3 registros
    E o último registro do histórico é:
      | ação | pagar      |
      | quem | tesouraria |
      | de   | Aprovado   |
      | para | Pago       |

  Cenário: 4.3 Imobiliário acima da alçada passa pela vistoria, aguarda a alçada e o aprovador libera
    Dado o processo da cota "COTA-004" da massa
    Quando os passos abaixo são executados em ordem:
      | quem  | ação                | motivo | resultado | situação          |
      | vitor | concluir a vistoria |        | aceita    | Aguardando alçada |
      | carla | aprovar na alçada   |        | aceita    | Aprovado          |
    Então o histórico tem 2 registros

  Cenário: 5.1 Reprovar sem motivo é recusado; com motivo, o motivo fica no histórico
    Dado o processo da cota "COTA-002" da massa
    Quando os passos abaixo são executados em ordem:
      | quem | ação     | motivo             | resultado | situação   | mensagem                                       |
      | ana  | reprovar |                    | recusada  | Em análise | Motivo é obrigatório para reprovar um processo |
      | ana  | reprovar | renda incompatível | aceita    | Reprovado  |                                                |
    Então o histórico tem 1 registro
    E o último registro do histórico é:
      | ação   | reprovar           |
      | quem   | ana                |
      | motivo | renda incompatível |

  Cenário: 8.1 O histórico guarda cada transição, com quem fez
    Dado o processo da cota "COTA-009" da massa
    Quando os passos abaixo são executados em ordem:
      | quem       | ação              | motivo | resultado | situação |
      | carla      | aprovar na alçada |        | aceita    | Aprovado |
      | tesouraria | pagar             |        | aceita    | Pago     |
    Então o histórico tem 2 registros
    E o último registro do histórico é:
      | ação | pagar      |
      | quem | tesouraria |
      | de   | Aprovado   |
