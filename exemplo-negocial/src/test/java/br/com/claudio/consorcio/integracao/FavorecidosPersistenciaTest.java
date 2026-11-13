package br.com.claudio.consorcio.integracao;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;

import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Os favorecidos gravados com o processo voltam do banco, na ordem informada, também depois de uma ação. */
@QuarkusTest
class FavorecidosPersistenciaTest {

    @Test
    void favorecidosSobrevivemAGravarEBuscar() {
        long cota = Api.criarCota(Api.criarCliente("Cláudia Favorecidos"));
        var corpo = Api.corpoDeProcesso(cota, "IMOBILIARIO", new BigDecimal("10000.00"), List.of(
                Api.favorecido("Cartório Central", "12345678000199", "cartório", new BigDecimal("500.00")),
                Api.favorecido("Vendedor", "12345678901", "vendedor", new BigDecimal("9500.00"))));
        long processo = Api.post("/processos", corpo).statusCode(201).extract().jsonPath().getLong("id");

        given().when().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Cartório Central", "Vendedor"))
                .body("documento", contains("12345678000199", "12345678901"))
                .body("papel", contains("cartório", "vendedor"))
                .body("valor", contains(500.0f, 9500.0f));

        // Salvar de novo (a ação grava o processo) não duplica nem perde os favorecidos.
        Api.acao(processo, "iniciar-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        given().when().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Cartório Central", "Vendedor"));
    }

    @Test
    void processoInexistenteNaoTemFavorecidos() {
        given().when().get("/processos/999999999/favorecidos").then().statusCode(404);
    }

    @Test
    void erroDeFormatoIndicaOFavorecidoEOCampo() {
        long cota = Api.criarCota(Api.criarCliente("Dora Formato"));
        var corpo = Api.corpoDeProcesso(cota, "SERVICO", new BigDecimal("100.00"), List.of(
                Api.favorecido("Ok", "12345678901", "vendedor", new BigDecimal("50.00")),
                Api.favorecido("Ruim", "123", "vendedor", new BigDecimal("50.00"))));

        Api.post("/processos", corpo).statusCode(400).body("campos.campo", hasItem("favorecidos[1].documento"));
    }

    @Test
    void favorecidoNuloNaListaE400() {
        long cota = Api.criarCota(Api.criarCliente("Edu Nulo"));
        var corpo = Api.corpoDeProcesso(cota, "SERVICO", new BigDecimal("100.00"),
                java.util.Collections.singletonList(null));

        Api.post("/processos", corpo).statusCode(400);
    }

    @Test
    void somaDiferenteDoValorE400ComMensagemENadaECriado() {
        long cota = Api.criarCota(Api.criarCliente("Fábio Soma"));
        var corpo = Api.corpoDeProcesso(cota, "SERVICO", new BigDecimal("100.00"),
                List.of(Api.favorecido("Ana", "12345678901", "vendedor", new BigDecimal("99.99"))));

        Api.post("/processos", corpo).statusCode(400);
        // A cota continua livre.
        Api.criarProcesso(cota, "SERVICO");
    }
}
