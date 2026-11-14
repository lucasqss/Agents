# Karate: execução das tabelas

Verificado com Karate 1.5.2, Java 25 e Quarkus 3.33.4, com o Enforcer (inclusive `dependencyConvergence`) ligado. Os arquivos citados estão em [`assets/`](../assets/), prontos para copiar (troque `PACOTE_RAIZ`).

## Dependências

```xml
<dependency><groupId>io.karatelabs</groupId><artifactId>karate-junit5</artifactId><version>1.5.2</version><scope>test</scope></dependency>
<!-- Simulador dos sistemas externos (empacotado, não conflita com o Quarkus) -->
<dependency><groupId>org.wiremock</groupId><artifactId>wiremock-standalone</artifactId><version>3.13.2</version><scope>test</scope></dependency>
```

O `*IT` precisa do `maven-failsafe-plugin` e do perfil `versao` ([pom obrigatório](../../arquiteto-padrao/references/pom-obrigatorio.md)).

**Não use o mock server do Karate** (`MockServer`) como simulador: ele depende de Netty 4.2, a BOM do Quarkus 3.33 fixa outra versão, e a aplicação não sobe (`NoClassDefFoundError: io/netty/channel/IoEventLoopGroup`). O Enforcer não pega, porque a BOM gerencia a versão.

## Runner: o par `*Test` e `*IT`

```java
@QuarkusTest
@WithTestResource(AntifraudeSimulado.class)
class PedidosAceitacaoTest {

    @Karate.Test
    Karate cenarios() {
        return Karate.run("classpath:aceitacao/pedidos/pedidos.feature")
                .systemProperty("aceitacao.porta", String.valueOf(RestAssured.port));
    }
}

@QuarkusIntegrationTest
class PedidosAceitacaoIT extends PedidosAceitacaoTest {
}
```

- `@Karate.Test` gera um teste JUnit por cenário, com o nome `"1.2 - Pedido acima do limite é recusado"` no relatório do Surefire.
- `RestAssured.port` é a porta da aplicação nas duas fases (no `*IT`, a do artefato empacotado). O `karate-config.js` a recebe:

```javascript
function fn() {
  var porta = karate.properties['aceitacao.porta'] || '8081';
  return { baseUrl: 'http://localhost:' + porta };
}
```

- O recurso de teste (`@WithTestResource`) sobe os simuladores antes da aplicação e entrega a ela as URLs. O `*IT` herda a anotação.

## A feature da capacidade

```gherkin
@setup
Scenario:
  * def clientes = read('massa/clientes.csv')
  * def linhas = read('cenarios.csv')
  * def tabelas = Java.type('PACOTE_RAIZ.aceitacao.Tabelas')
  * def cenarios = tabelas.agrupar(linhas, { cliente: clientes })

Scenario Outline: <cenario> - <descricao>
  * def clientes = read('massa/clientes.csv')
  * def massa = { cliente: '#(clientes)' }
  * def contexto = {}
  * karate.forEach(passos, function(p){ karate.call('passos.feature@' + p.acao, { passo: p, massa: massa, contexto: contexto }) })

  Examples:
    | karate.setup().cenarios |
```

- O `@setup` roda uma vez. `Tabelas.agrupar` confere, para cada coluna do mapa (`cliente`), que toda chave existe na massa, e agrupa os passos por `cenario`. Referência quebrada falha aqui: `cenarios.csv, linha 8: cliente 'carla' não existe na massa`.
- Em literal JSON do Karate, chamada de função não é avaliada: use `'#(variavel)'` (expressão embutida), como em `{ cliente: '#(clientes)' }`.
- `contexto` é compartilhado entre os passos do mesmo cenário (o id do pedido criado no passo 1 é usado no passo 2).

## `passos.feature`: uma cena por ação

```gherkin
@ignore
Feature: Passos dos cenários de pedidos

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
```

- `@ignore` impede que o arquivo rode sozinho.
- Valores do CSV chegam como texto: `* 1` converte para número.
- Todo passo confere o efeito por outro endpoint (`GET` depois do `POST`).
- Ação nova: uma cena nova com a tag. As linhas da tabela passam a poder usá-la.

## Simulador de sistema externo

`AntifraudeSimulado` (em `assets/exemplo-pedidos/java/`) lê `massa/externa-antifraude.csv` com `Massa.ler` e registra uma resposta do WireMock por linha. Cenário que precisa de outra resposta ganha outra linha na massa externa, não código novo.

## Relatório

`target/karate-reports/<capacidade>.html`: cada cenário, cada passo, requisição e resposta. Em falha, a linha do `match` mostra esperado e obtido; o nome do teste traz o `cenario` da tabela.

## Erros comuns

| Sintoma | Causa |
|---|---|
| `NoClassDefFoundError: io/netty/channel/IoEventLoopGroup` ao subir | Uso do `MockServer` do Karate. Use WireMock |
| `js failed ... Index: 0` em `karate.filter(...)[0]` | A chave não está na massa, ou a massa chegou vazia (função em literal JSON sem `'#(...)'`) |
| Todos os cenários falham com conexão recusada | A porta não chegou ao `karate-config.js`; confira o `systemProperty` no runner |
| `Port already bound: 8081` | Outro build de teste na máquina; rode com `-Dquarkus.http.test-port=0` |
| Número de cenários diferente entre `*Test` e `*IT` | Um cenário foi desligado numa das fases |
