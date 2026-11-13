package br.com.claudio.base;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

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

    @Test
    void corpoDasAcoesDoProcessoEhOpcional() {
        for (String acao : new String[] {"iniciar-analise", "concluir-analise", "concluir-vistoria",
                "receber-documento"}) {
            given().when().get("/q/openapi?format=json").then()
                    .statusCode(200)
                    .body("paths.'/processos/{id}/" + acao + "'.post.requestBody.required",
                            anyOf(nullValue(), equalTo(false)));
        }
    }

    @Test
    void corpoDasAcoesComMotivoEhObrigatorio() {
        for (String acao : new String[] {"solicitar-documento", "reprovar", "devolver-para-analise", "cancelar"}) {
            given().when().get("/q/openapi?format=json").then()
                    .statusCode(200)
                    .body("paths.'/processos/{id}/" + acao + "'.post.requestBody.required", equalTo(true));
        }
    }
}
