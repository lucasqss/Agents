# Par `*Test` e `*IT` no Quarkus

Guia de referência: [Testing your application, Quarkus 3.33](https://quarkus.io/version/3.33/guides/getting-started-testing/#quarkus-integration-test). O comportamento descrito aqui foi verificado com Quarkus 3.33.4.

## O padrão

`src/test/java/<pacote-base>/aceitacao/<capacidade>/<Capacidade>AceitacaoTest.java`

```java
@QuarkusTest
class PagamentoAceitacaoTest {

    @Test
    void pagamentoFicaRegistrado() {
        // Só HTTP, com RestAssured.
    }
}
```

`src/test/java/<pacote-base>/aceitacao/<capacidade>/<Capacidade>AceitacaoIT.java`

```java
@QuarkusIntegrationTest
class PagamentoAceitacaoIT extends PagamentoAceitacaoTest {
    // Roda os mesmos testes. O corpo fica vazio.
}
```

| | `*Test` | `*IT` |
|---|---|---|
| Anotação | `@QuarkusTest` | `@QuarkusIntegrationTest` |
| Plugin Maven | surefire | failsafe |
| Fase | `test` | `integration-test` e `verify` |
| Quando roda | todo `mvn verify` | só com `-Pversao` (geração de versão) |
| O que roda | aplicação na JVM do teste | o artefato empacotado, em **outro processo** |
| Perfil | `test` | `prod` |

O pom precisa do `maven-failsafe-plugin` com os goals `integration-test` e `verify`. Sem isso, o `*IT` não roda e nenhum erro aparece. O failsafe respeita `skipITs`: o pom o deixa `true` por padrão, para o `mvn verify` do dia a dia não repetir a aceitação contra o pacote, e o perfil `versao` o desliga. O par `*Test`/`*IT` continua obrigatório; só o momento de rodar o `*IT` muda.

```xml
<plugin>
    <artifactId>maven-failsafe-plugin</artifactId>
    <version>3.5.6</version>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>
                <goal>verify</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <argLine>@{argLine}</argLine>
        <systemPropertyVariables>
            <java.util.logging.manager>org.jboss.logmanager.LogManager</java.util.logging.manager>
            <maven.home>${maven.home}</maven.home>
        </systemPropertyVariables>
    </configuration>
</plugin>
```

E, no mesmo pom, a propriedade e o perfil:

```xml
<properties>
    <!-- Os *IT repetem os testes de aceitação contra o pacote final e rodam só na geração de versão (-Pversao) -->
    <skipITs>true</skipITs>
</properties>

<profiles>
    <profile>
        <id>versao</id>
        <properties>
            <skipITs>false</skipITs>
        </properties>
    </profile>
</profiles>
```

Dependências de teste:

```xml
<dependency><groupId>io.quarkus</groupId><artifactId>quarkus-junit5</artifactId><scope>test</scope></dependency>
<dependency><groupId>io.rest-assured</groupId><artifactId>rest-assured</artifactId><scope>test</scope></dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock-standalone</artifactId>
    <version>3.13.2</version>
    <scope>test</scope>
</dependency>
```

## Por que o `*IT` força a caixa-preta

| Não funciona no `*IT` | Motivo |
|---|---|
| `@Inject` e `@InjectMock` no teste | A aplicação está em outro processo |
| Propriedades `%test.*` e `src/test/resources/application.properties` | O perfil é `prod` |
| Recurso JAX-RS em `src/test` como stub | Não entra no artefato empacotado |
| Variável estática para mudar o comportamento da aplicação | Cada processo tem as suas |

Se o teste respeita as regras da skill, nada disso faz falta.

## Banco de dados

Se o projeto usa Dev Services, o banco sobe sozinho também no `*IT`. O teste não acessa o banco: cada caso cria seus dados pelos endpoints e usa valores únicos. O banco é compartilhado por todos os casos, então nunca conte registros de forma absoluta.

## Sistema externo

Simule com WireMock, em uma classe do ambiente de teste, na JVM do teste. Funciona nas duas fases porque a aplicação chama o stub por HTTP. Use `org.wiremock:wiremock-standalone` em escopo de teste.

`src/test/java/<pacote-base>/aceitacao/<Sistema>StubResource.java`

```java
public class SistemaExternoStubResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        // Propriedade de sistema: o stub e o teste não dependem de carregar as mesmas classes.
        System.setProperty("aceitacao.sistema-externo.porta", String.valueOf(servidor.port()));
        return Map.of("quarkus.rest-client.sistema-externo.url", servidor.baseUrl());
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
```

No `*Test`:

```java
@QuarkusTest
@WithTestResource(SistemaExternoStubResource.class)
class PagamentoAceitacaoTest { }
```

O `*IT` herda o `@WithTestResource` e não precisa repetir a anotação. O mapa devolvido por `start()` chega à aplicação nas duas fases e vence a configuração `%test.*` da mesma chave.

**Controle a resposta do stub por HTTP**, em uma classe com linguagem de negócio. Devolva um valor que só o stub emite. Encontrá-lo na resposta prova que a chamada passou pelo stub.

```java
public static void ficarIndisponivel() {
    WireMock cliente = new WireMock("localhost", Integer.getInteger("aceitacao.sistema-externo.porta"));
    cliente.resetMappings();
    cliente.register(post(urlEqualTo("/recurso-externo")).willReturn(aResponse().withStatus(503)));
}
```

Configure a situação normal em um `@BeforeEach`, para um caso não afetar o seguinte.

## Rodar

```bash
./mvnw verify            # domínio, ArchUnit e @QuarkusTest (inclusive aceitação) e empacota
./mvnw verify -Pversao    # o anterior + os *IT contra o artefato empacotado (geração de versão)
```

Se o `mvnw` não executar no Linux ou WSL com `cannot execute: required file not found`, o arquivo está com fim de linha CRLF. Use o `mvn` instalado, com o `JAVA_HOME` no JDK do projeto.

## Falhas comuns

| Sintoma | Causa |
|---|---|
| `IllegalStateException: Expected response body to be verified as JSON, HTML or XML but no content-type was defined` | A resposta veio sem corpo, e o teste esperava um campo. Compare com o contrato: pode ser defeito |
| O `*IT` não roda no `mvn verify` | Esperado: o `skipITs` é `true` por padrão. Rode com `-Pversao` |
| O `*IT` não aparece no relatório mesmo com `-Pversao` | Falta o failsafe, ou o nome não termina em `IT` |
| Quantidade de testes difere entre as fases | Algum teste foi desligado no `*IT` |
| Chamada ao sistema externo falha só no `*IT` | O stub está em `src/test` como recurso JAX-RS |
