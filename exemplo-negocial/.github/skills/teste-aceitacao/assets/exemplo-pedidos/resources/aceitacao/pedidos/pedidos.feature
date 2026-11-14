Feature: Pedidos (aceitação guiada por tabela)

  # Lê a massa e os cenários, confere as chaves (referência quebrada falha aqui, com a linha do CSV) e agrupa os passos
  # de cada cenário. Roda uma vez. Cada coluna de cenarios.csv que aponta para a massa entra no mapa passado a agrupar.
  @setup
  Scenario:
    * def clientes = read('massa/clientes.csv')
    * def linhas = read('cenarios.csv')
    * def tabelas = Java.type('PACOTE_RAIZ.aceitacao.Tabelas')
    * def cenarios = tabelas.agrupar(linhas, { cliente: clientes })

  # Um teste por cenário. Os passos rodam em ordem; cada um chama a cena de passos.feature com a tag da sua "acao".
  Scenario Outline: <cenario> - <descricao>
    * def clientes = read('massa/clientes.csv')
    * def massa = { cliente: '#(clientes)' }
    * def contexto = {}
    * karate.forEach(passos, function(p){ karate.call('passos.feature@' + p.acao, { passo: p, massa: massa, contexto: contexto }) })

    Examples:
      | karate.setup().cenarios |
