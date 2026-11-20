@ignore
Feature: Passos dos cenários de processo: cada passo vira uma requisição, e o efeito é conferido por outro endpoint

  Background:
    * url baseUrl

  # Abre um processo novo. O ID gerado é conferido quando o cenário o espera (espera.id): com a massa de IDs fixos e os
  # cenários sempre na mesma ordem, ele é o mesmo em todo build.
  @abrir
  Scenario: abrir processo
    Given path 'processos'
    And request passo.dados
    When method post
    Then match responseStatus == passo.espera.status
    * def idEsperado = passo.espera.id ? passo.espera.id : '#ignore'
    And match response.id == idEsperado
    * contexto.processo = response.id
    * if (passo.espera.estado) karate.call('passos.feature@conferir', { id: contexto.processo, espera: passo.espera })

  # Executa uma ação no processo da massa (passo.processo) ou no que o cenário abriu antes. Depois de toda ação, aceita
  # ou recusada, confere a situação gravada.
  @executar
  Scenario: executar ação
    * def id = passo.processo ? passo.processo : contexto.processo
    # Dentro de expressão JS (o ternário), '#(...)' não é avaliado: use a variável direto.
    * def corpo = passo.motivo ? { motivo: passo.motivo } : {}
    * def usuario = passo.usuario ? passo.usuario : 'ana'
    Given path 'processos', id, 'acoes', passo.comando
    And header X-Usuario = usuario
    And request corpo
    When method post
    Then match responseStatus == passo.espera.status
    * def mensagemEsperada = passo.espera.mensagem ? passo.espera.mensagem : '#ignore'
    And match response.mensagem == mensagemEsperada
    * karate.call('passos.feature@conferir', { id: id, espera: passo.espera })

  # Confere, por outros endpoints, a situação gravada e, se o cenário pede, as ações disponíveis e o histórico.
  @conferir
  Scenario: conferir efeito
    Given path 'processos', id
    When method get
    Then status 200
    And match response.estado == espera.estado
    * if (espera.acoes) karate.call('passos.feature@acoes', { id: id, acoes: espera.acoes })
    * if (espera.historico) karate.call('passos.feature@historico', { id: id, historico: espera.historico })

  @acoes
  Scenario: conferir ações disponíveis
    Given path 'processos', id, 'acoes'
    When method get
    Then status 200
    And match response == acoes

  # historico.quantidade: quantas transições o processo tem; historico.ultima: campos da última (acao, autor, observacao...).
  @historico
  Scenario: conferir histórico
    Given path 'processos', id, 'historico'
    When method get
    Then status 200
    * def tamanho = '#[' + historico.quantidade + ']'
    And match response == tamanho
    * def ultima = response[response.length - 1]
    And match ultima contains historico.ultima
