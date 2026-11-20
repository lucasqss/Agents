function fn() {
  // Porta da aplicação em teste: a do @QuarkusTest ou a do artefato empacotado (*IT), entregue pelo runner.
  var porta = karate.properties['aceitacao.porta'] || '8081';
  return { baseUrl: 'http://localhost:' + porta };
}
