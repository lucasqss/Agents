@ignore
Feature: Passos dos cenários de pedidos: cada linha do CSV vira uma requisição, e o efeito é conferido por outro endpoint

  Background:
    * url baseUrl

  @criar
  Scenario: criar pedido
    * def linha = karate.filter(massa.cliente, function(c){ return c.chave == passo.cliente })[0]
    Given path 'clientes'
    And request { nome: '#(linha.nome)', documento: '#(linha.documento)', limite: '#(linha.limite * 1)' }
    When method post
    Then status 201
    Given path 'pedidos'
    And request { clienteId: '#(response.id)', valor: '#(passo.valor * 1)' }
    When method post
    Then match responseStatus == passo.status * 1
    * contexto.pedido = response.id
    Given path 'pedidos', contexto.pedido
    When method get
    Then match response.estado == passo.estado

  @cancelar
  Scenario: cancelar o pedido do cenário
    Given path 'pedidos', contexto.pedido, 'cancelar'
    And request {}
    When method post
    Then match responseStatus == passo.status * 1
    Given path 'pedidos', contexto.pedido
    When method get
    Then match response.estado == passo.estado
