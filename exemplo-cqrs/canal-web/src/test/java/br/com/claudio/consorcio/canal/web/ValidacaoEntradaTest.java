package br.com.claudio.consorcio.canal.web;

import static br.com.claudio.consorcio.canal.web.Api.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Entrada fora do formato vira 400 com os campos, antes de chegar ao use case (e nunca 500). */
@QuarkusTest
class ValidacaoEntradaTest {

    @Test
    void clienteSemCamposObrigatorios() {
        Api.post("/web/v1/clientes", Map.of()).statusCode(400)
                .body("mensagem", equalTo("Dados inválidos"))
                .body("campos.campo", hasItems("nome", "cpf"))
                .body("campos.erro", hasItem("não deve estar em branco"));
    }

    @Test
    void nomeMaiorQueAColunaNaoChegaAoBanco() {
        Api.post("/web/v1/clientes", Map.of("nome", "x".repeat(201), "cpf", Api.unico("C"))).statusCode(400)
                .body("campos.campo", hasItem("nome"));
    }

    @Test
    void processoSemCorpo() {
        given().contentType(ContentType.JSON).when().post("/web/v1/processos").then().statusCode(400)
                .body("mensagem", equalTo("Dados inválidos"));
    }

    @Test
    void cotaComValorNegativo() {
        long cliente = Api.criarCliente("Fábio");
        Api.post("/web/v1/cotas", Map.of("numero", Api.unico("COTA-"), "valorCredito", -1, "clienteId", cliente))
                .statusCode(400)
                .body("campos.campo", hasItem("valorCredito"));
    }

    @Test
    void restricoesPublicadasNoOpenApi() {
        given().queryParam("format", "json").when().get("/q/openapi").then().statusCode(200)
                .body("components.schemas.ClienteRequest.required", hasItems("nome", "cpf"))
                .body("components.schemas.ClienteRequest.properties.nome.maxLength", equalTo(200));
    }
}
