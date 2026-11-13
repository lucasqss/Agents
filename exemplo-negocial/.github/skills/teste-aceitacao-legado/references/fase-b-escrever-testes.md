# Fase B: escrever e rodar os testes

Os nomes `pedido` e `Pedido` abaixo são ilustrativos. Troque pelos da capacidade.

## B2. Conferir o projeto antes de escrever

```bash
grep -n "maven-failsafe-plugin\|rest-assured\|wiremock\|quarkus-junit" pom.xml
find src/test -path "*aceitacao*" -name "*.java"
```

- **Sem `maven-failsafe-plugin`:** o `*IT` não roda e nenhum erro aparece. Pare e mostre ao usuário o bloco abaixo antes de alterar o pom.
- **Já existe `ApiAceitacao` ou `*Stub`:** reaproveite os métodos de mecânica (criar dados, enviar, consultar). Não copie os resultados esperados de outros testes.

```xml
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>integration-test</goal><goal>verify</goal></goals>
        </execution>
    </executions>
    <configuration>
        <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

## B3. Os arquivos

| Arquivo | Onde |
|---|---|
| `caso-de-uso.md` | `src/test/resources/aceitacao/<capacidade>/` |
| `<Capacidade>AceitacaoTest.java` e `<Capacidade>AceitacaoIT.java` | `src/test/java/<pacote-base>/aceitacao/<capacidade>/` |
| Atalhos HTTP compartilhados | `src/test/java/<pacote-base>/aceitacao/ApiAceitacao.java` |
| Stub de sistema externo | `src/test/java/<pacote-base>/aceitacao/<Sistema>Stub.java` e `<Sistema>StubResource.java` |

`<capacidade>` é o nome do negócio em minúsculas, sem acento: `pagamentoprocesso`, `cadastrocliente`. Nunca `controller` ou `service`.

### Teste

```java
@QuarkusTest
class PedidoAceitacaoTest {

    @Test
    @DisplayName("Pedido válido é aberto e aparece na consulta")       // C1 (R1)
    void pedidoValidoEAberto() {
        String codigo = ApiAceitacao.unico("PED");                       // dado próprio do caso

        long id = ApiAceitacao.enviar("/pedidos", Map.of("codigo", codigo, "valor", 100))
                .statusCode(201)                                          // resultado esperado: um só
                .extract().jsonPath().getLong("id");

        Map<String, Object> pedido = ApiAceitacao.buscarNaListagem("/pedidos", id);   // efeito, por outro endpoint
        assertThat(pedido.get("codigo"), equalTo(codigo));
    }

    @Test
    @DisplayName("Valor zero é recusado e nada é gravado")             // C4 (R2)
    void valorZeroERecusado() {
        String codigo = ApiAceitacao.unico("PED");

        ApiAceitacao.enviar("/pedidos", Map.of("codigo", codigo, "valor", 0))
                .statusCode(400)
                .body("campos.campo", hasItem("valor"));

        assertThat(ApiAceitacao.existeNaListagem("/pedidos", "codigo", codigo), equalTo(false));
    }
}
```

- Comentário `// Cn (Rn)` em cada teste: é a rastreabilidade.
- Suspeito fixado como está: o `@DisplayName` começa por `[atual]`.
- Tabela situação × ação: um `@ParameterizedTest` com `@MethodSource` que gera uma linha por célula. Não escreva um método por célula.

### Par de integração: corpo vazio, sempre

```java
@QuarkusIntegrationTest
class PedidoAceitacaoIT extends PedidoAceitacaoTest {
}
```

### Atalhos HTTP: só RestAssured, sem classe de produção

```java
public final class ApiAceitacao {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    private ApiAceitacao() {
    }

    public static String unico(String prefixo) {
        return prefixo + "-" + SEQUENCIA.incrementAndGet();
    }

    public static ValidatableResponse enviar(String caminho, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(caminho).then();
    }

    public static ValidatableResponse alterar(String caminho, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().put(caminho).then();
    }

    /** Sem consulta por id: procura o item pelo id na listagem. */
    public static Map<String, Object> buscarNaListagem(String caminho, long id) {
        List<Map<String, Object>> itens = given().when().get(caminho).then().statusCode(200)
                .extract().jsonPath().getList("$");
        return itens.stream()
                .filter(item -> ((Number) item.get("id")).longValue() == id)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Item %d não aparece em %s".formatted(id, caminho)));
    }

    public static boolean existeNaListagem(String caminho, String campo, Object valor) {
        List<Map<String, Object>> itens = given().when().get(caminho).then().statusCode(200)
                .extract().jsonPath().getList("$");
        return itens.stream().anyMatch(item -> valor.equals(item.get(campo)));
    }
}
```

Se a resposta for um objeto (`itens`) ou o id for texto, ajuste o caminho do JSON (`getList("itens")`). Confira o formato no resource, e não em outro teste.

**Listagem paginada:** sem percorrer as páginas, o item pode não aparecer. Use esta versão, com os nomes dos parâmetros do resource:

```java
public static Map<String, Object> buscarNaListagemPaginada(String caminho, long id) {
    for (int pagina = 1; pagina < 1000; pagina++) {
        List<Map<String, Object>> itens = given().queryParam("pagina", pagina).queryParam("tamanho", 100)
                .when().get(caminho).then().statusCode(200).extract().jsonPath().getList("itens");
        if (itens.isEmpty()) {
            break;
        }
        for (Map<String, Object> item : itens) {
            if (((Number) item.get("id")).longValue() == id) {
                return item;
            }
        }
    }
    throw new AssertionError("Item %d não aparece em %s".formatted(id, caminho));
}
```

Se a primeira página começar em 0, comece o laço em 0.

### Sistema externo (só se a capacidade chama um)

```java
public class SistemaStubResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        servidor.start();
        System.setProperty("aceitacao.sistema.porta", String.valueOf(servidor.port()));
        return Map.of("quarkus.rest-client.<nome-do-client>.url", servidor.baseUrl());
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
```

- `<nome-do-client>` é a `configKey` do `@RegisterRestClient`.
- O teste usa `@WithTestResource(SistemaStubResource.class)`, que o `*IT` herda.
- No `<Sistema>Stub`, métodos com nome de negócio (`ficarDisponivel()`, `ficarIndisponivel()`) chamam `new WireMock("localhost", Integer.getInteger("aceitacao.sistema.porta"))`.
- Chame `ficarDisponivel()` num `@BeforeEach`.
- **Client com `@CircuitBreaker`:** o circuito é compartilhado por todos os casos da mesma execução. Falhas demais abrem o circuito, e os casos seguintes recebem 503 sem motivo. Mantenha os casos de falha abaixo do `requestVolumeThreshold` da anotação. Se o plano precisar de mais casos que isso, pergunte ao usuário antes de escrever.
- **Client com `@Retry` e `@Timeout`:** um caso de timeout espera timeout × (retries + 1). Prefira simular falha com status 503 do stub, que responde na hora.

## Regras da fase B

- **Só HTTP.** Sem `@Inject`, `@InjectMock`, `QuarkusMock`, banco ou import de `src/main`. Os testes declaram os próprios payloads (`Map.of(...)`).
- **Dados únicos** em cada caso (`unico(...)`). O banco é compartilhado: nunca confira total, contagem ou ordem da lista inteira; confira só os itens criados no caso.
- **Cada caso tem um só resultado esperado**, o do plano aprovado.

## B4. Rodar

```bash
./mvnw verify
```

Se o `mvnw` falhar com `required file not found`, ele está com fim de linha CRLF: use `mvn verify`.

## B5. Quando falhar, classifique antes de mexer

| A falha é | Exemplo | O que fazer |
|---|---|---|
| **Mecânica do teste** | não compila, caminho do JSON errado, dado repetido, falta import | Corrija **o teste**, sem mudar o resultado esperado |
| **O sistema diverge do plano aprovado** | o plano diz 409, e o sistema responde 500 | É **defeito ou erro de leitura**. Releia o trecho da origem. Não mude o esperado. Registre na seção Defeitos, deixe vermelho, **não faça commit** e relate ao usuário |
| **Status sem corpo** | `Expected response body to be verified as JSON... no content-type` | O teste esperava um campo e a resposta veio vazia: é divergência do plano, não mecânica |
| **Só o `*IT` falha** | chamada externa falha | O stub não pode ser um resource JAX-RS em `src/test`: use o WireMock acima |

**Mudar o resultado esperado exige uma nova aprovação do usuário.** "Fazer passar" nunca é motivo.

## B6. Sessão exploratória

1. Escreva a missão em uma frase. Ex.: "procurar respostas que contrariem as regras do plano".
2. Suba o sistema empacotado (`java -jar target/quarkus-app/quarkus-run.jar`, com o banco das instruções do repositório) ou use `./mvnw quarkus:dev`.
3. Só com `curl`, tente: corpo vazio, JSON malformado, campo extra, tipo errado, enum em minúsculas, id inexistente ou negativo, ação repetida, ação depois do estado final e dois pedidos simultâneos.
4. Anote cada tentativa na seção 9 do `caso-de-uso.md`.
5. Contradiz o plano: defeito. O plano não diz nada: pergunta. **Não crie teste para comportamento não aprovado.**

## B7. Antes de terminar

- [ ] `./mvnw verify` verde, ou vermelho só por defeito registrado e relatado.
- [ ] `target/surefire-reports` e `target/failsafe-reports` têm o **mesmo número** de testes de aceitação.
- [ ] Este comando não mostra nada (nenhum import de produção):
  `grep -h "^import" $(find src/test -path "*aceitacao*" -name "*.java") | grep -v "\.aceitacao\.\|io\.quarkus\.test\|io\.restassured\|org\.junit\|org\.hamcrest\|java\.\|com\.github\.tomakehurst"`
- [ ] O `caso-de-uso.md` tem a situação `CONFIRMADO` e as seções 7 a 10 preenchidas.
- [ ] O relato ao usuário traz os defeitos, os suspeitos fixados, as dúvidas abertas e a exploração.
