# Mecânica do `@QuarkusTest`

Guia de referência: [Testing your application, Quarkus 3.33](https://quarkus.io/version/3.33/guides/getting-started-testing/). Verificado com Quarkus 3.33.4.

## O teste

```java
@QuarkusTest
class ValidacaoEntradaTest {

    @Test
    void pedidoSemCamposObrigatorios() {
        given().contentType(ContentType.JSON).body(Map.of()).when().post("/pedidos").then()
                .statusCode(400)
                .body("campos.campo", hasItems("contaId", "valor"));
    }
}
```

- A aplicação sobe uma vez e é reaproveitada pelos testes com a mesma configuração. Um `@TestProfile` diferente reinicia a aplicação: use pouco.
- RestAssured já aponta para a porta de teste (`quarkus.http.test-port`, padrão `8081`). Dois builds na mesma máquina disputam a porta; rode com `-Dquarkus.http.test-port=0` nesse caso.
- Centralize a montagem repetida (criar conta, criar pedido, dar um valor único) numa classe de apoio do pacote, como a `Api` do exemplo (`integracao/Api.java`).

## Banco

O Dev Services sobe o PostgreSQL em um contêiner (precisa de Docker). As migrations do Flyway rodam ao subir, então um teste qualquer prova que a migration nova aplica. O banco é **compartilhado** por todos os testes da execução: cada caso cria os seus dados com valores únicos e nunca conta registros de forma absoluta.

## Persistência

Crie pelo endpoint de escrita e leia pelo de leitura. Confira **todos** os campos, inclusive os que o mapper converte (enum, valor monetário, data, campo opcional vazio). Um campo esquecido no mapper só aparece aqui.

## Tradução de erro

Um caso por tradução, no formato de erro padrão (`{ "mensagem": ..., "campos": [...] }`):

| Situação | Status |
|---|---|
| Formato da entrada (Bean Validation) | `400`, com os campos |
| Regra de domínio recusada | `400` ou `409`, conforme o mapeamento de exceções do projeto |
| Recurso inexistente | `404` |
| Conflito, transição inválida, versão desatualizada | `409` |
| Sistema externo indisponível | `503` |

## Concorrência

Dispare as operações juntas, com uma barreira (`CyclicBarrier`) para que cheguem ao mesmo tempo, e confira que o efeito aconteceu **uma vez**: um `200` e os demais `409`, e o estado final coerente lido por outro endpoint. Modelo: `integracao/ConcorrenciaAlteracaoTest.java` e `ConcorrenciaProcessoTest.java` no exemplo.

## Canal

Num `*Response` de canal (web, mobile, terminal), confira os **nomes exatos** dos campos e os textos formatados. Renomear um campo quebra o cliente; o teste de canal e o [contrato](contrato.md) seguram isso.

## Falhas comuns

| Sintoma | Causa |
|---|---|
| `Port already bound: 8081` | Outro processo de teste na mesma máquina. Rode com `-Dquarkus.http.test-port=0` |
| Teste passa sozinho e falha na suíte | Depende de dado de outro teste ou de contagem absoluta |
| `Expected response body to be verified as JSON ... but no content-type` | A resposta veio sem corpo; compare com o contrato, pode ser defeito |
| Os `@QuarkusTest` abortam com "Quarkus JUnit configuration has been overridden" | Existe um `junit-platform.properties` no projeto. Remova; configure o jqwik pelo Surefire |
