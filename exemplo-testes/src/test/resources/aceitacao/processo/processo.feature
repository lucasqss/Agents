Feature: Processo de pagamento (aceitação guiada por cenários em YAML, com massa de IDs fixos)

  # Antes de tudo, a sentinela: o processo 1 da massa, que nenhum cenário usa, tem de estar lá como foi gravado. Se não
  # está, a aplicação não subiu sobre o banco da aceitação (BancoDaAceitacao) e nenhum ID esperado vale: para aqui.
  # Depois, lê os cenários e confere as referências à massa: ID fora da massa, reservado ou usado por dois cenários falha.
  @setup
  Scenario:
    Given url baseUrl
    And path 'processos', 1
    When method get
    * def sentinela = responseStatus == 200 && response.numeroCota == 'COTA-SENTINELA' && response.estado == 'ABERTO'
    * if (!sentinela) karate.fail('A aplicação não está sobre o banco da aceitação (BancoDaAceitacao): o processo 1 da massa não foi encontrado como gravado. Resposta: ' + responseStatus + ' ' + karate.pretty(response))
    * def arquivo = read('cenarios.yaml')
    * def cenarios = Java.type('br.com.claudio.pagamento.aceitacao.Cenarios').validar(arquivo.cenarios, 'processo', [1])

  # Um teste por cenário, na ordem do arquivo. Cada passo chama a cena de passos.feature com a tag da sua "acao".
  Scenario Outline: <cenario> - <descricao>
    * def contexto = {}
    * karate.forEach(passos, function(p){ karate.call('passos.feature@' + p.acao, { passo: p, contexto: contexto }) })

    Examples:
      | karate.setup().cenarios |
