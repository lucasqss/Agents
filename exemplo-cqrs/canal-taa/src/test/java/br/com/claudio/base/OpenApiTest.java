package br.com.claudio.base;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/** Teste de fumaça: a aplicação sobe e publica o documento OpenAPI. */
@QuarkusTest
class OpenApiTest {

    @Test
    void publicaDocumentoOpenApi() {
        given().when().get("/q/openapi").then()
                .statusCode(200)
                .body(containsString("openapi:"));
    }
}
