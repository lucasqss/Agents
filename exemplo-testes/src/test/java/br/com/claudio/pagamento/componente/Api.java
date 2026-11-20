package br.com.claudio.pagamento.componente;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.util.Map;

/** Atalhos de RestAssured para os testes de componente: só HTTP, nenhuma classe de produção. */
final class Api {

    static final String USUARIO = "X-Usuario";

    static {
        // Em falha, o relatório mostra a requisição e a resposta inteiras, não só o status.
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    private Api() {
    }

    static ValidatableResponse abrir(String tipo, String cota, String valor) {
        return RestAssured.given().contentType(ContentType.JSON)
                .body(Map.of("tipo", tipo, "numeroCota", cota, "valor", new BigDecimal(valor)))
                .when().post("/processos").then();
    }

    static long abrirOk(String tipo, String valor) {
        return abrir(tipo, "COTA-" + System.nanoTime() % 1_000_000, valor).statusCode(201).extract().jsonPath().getLong("id");
    }

    static ValidatableResponse executar(long id, String acao, String usuario) {
        return RestAssured.given().header(USUARIO, usuario).when().post("/processos/{id}/acoes/{acao}", id, acao).then();
    }

    static ValidatableResponse executar(long id, String acao, String usuario, String motivo) {
        return RestAssured.given().header(USUARIO, usuario).contentType(ContentType.JSON).body(Map.of("motivo", motivo))
                .when().post("/processos/{id}/acoes/{acao}", id, acao).then();
    }

    static ValidatableResponse get(String caminho, Object... parametros) {
        return RestAssured.given().when().get(caminho, parametros).then();
    }
}
