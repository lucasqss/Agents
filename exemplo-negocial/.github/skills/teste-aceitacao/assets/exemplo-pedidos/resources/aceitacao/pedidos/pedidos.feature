Feature: Pedidos (aceitação guiada por massa e cenários em YAML)

  # Lê a massa e os cenários e confere as chaves: referência a massa inexistente falha aqui, dizendo cenário e passo.
  # Cada campo dos passos que aponta para uma massa entra no mapa passado a validar. Roda uma vez.
  @setup
  Scenario:
    * def clientes = read('massa/clientes.yaml')
    * def arquivo = read('cenarios.yaml')
    * def tabelas = Java.type('PACOTE_RAIZ.aceitacao.Tabelas')
    * def cenarios = tabelas.validar(arquivo.cenarios, { cliente: clientes })

  # Um teste por cenário. Os passos rodam em ordem; cada um chama a cena de passos.feature com a tag da sua "acao".
  Scenario Outline: <cenario> - <descricao>
    * def clientes = read('massa/clientes.yaml')
    * def massa = { cliente: '#(clientes)' }
    * def contexto = {}
    * karate.forEach(passos, function(p){ karate.call('passos.feature@' + p.acao, { passo: p, massa: massa, contexto: contexto }) })

    Examples:
      | karate.setup().cenarios |
