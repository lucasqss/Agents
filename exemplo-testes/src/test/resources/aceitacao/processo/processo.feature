Feature: Processo de pagamento (aceitação guiada por cenários em YAML, com massa de IDs fixos)

  # Lê os cenários e confere as referências à massa: ID fora da massa, ou usado por dois cenários, falha aqui.
  @setup
  Scenario:
    * def arquivo = read('cenarios.yaml')
    * def cenarios = Java.type('br.com.claudio.pagamento.aceitacao.Cenarios').validar(arquivo.cenarios, 'processo')

  # Um teste por cenário, na ordem do arquivo. Cada passo chama a cena de passos.feature com a tag da sua "acao".
  Scenario Outline: <cenario> - <descricao>
    * def contexto = {}
    * karate.forEach(passos, function(p){ karate.call('passos.feature@' + p.acao, { passo: p, contexto: contexto }) })

    Examples:
      | karate.setup().cenarios |
