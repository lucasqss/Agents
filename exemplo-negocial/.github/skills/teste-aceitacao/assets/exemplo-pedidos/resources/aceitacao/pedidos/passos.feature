@ignore
Feature: Passos dos cenários de pedidos: cada passo vira uma requisição, e o efeito é conferido por outro endpoint

  Background:
    * url baseUrl

  @criar
  Scenario: criar pedido
    * def cliente = karate.filter(massa.cliente, function(c){ return c.chave == passo.cliente })[0]
    Given path 'clientes'
    And request { nome: '#(cliente.nome)', documento: '#(cliente.documento)', limite: '#(cliente.limite)' }
    When method post
    Then status 201
    Given path 'pedidos'
    And request { clienteId: '#(response.id)', valor: '#(passo.valor)', itens: '#(passo.itens)' }
    When method post
    Then match responseStatus == passo.espera.status
    * contexto.pedido = response.id
    Given path 'pedidos', contexto.pedido
    When method get
    Then match response.estado == passo.espera.estado
    And match response.itens == passo.itens

  @cancelar
  Scenario: cancelar o pedido do cenário
    Given path 'pedidos', contexto.pedido, 'cancelar'
    And request {}
    When method post
    Then match responseStatus == passo.espera.status
    Given path 'pedidos', contexto.pedido
    When method get
    Then match response.estado == passo.espera.estado
