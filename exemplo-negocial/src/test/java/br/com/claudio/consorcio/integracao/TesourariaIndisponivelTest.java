package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
class TesourariaIndisponivelTest {

    @AfterEach
    void religarTesouraria() {
        TesourariaFake.falhar = false;
    }

    @Test
    void falhaDaTesourariaDesfazOPagamento() {
        long cota = Api.criarCota(Api.criarCliente("Eva"));
        long processo = Api.criarProcesso(cota, "SERVICO");
        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200);

        TesourariaFake.falhar = true;
        acao(processo, "pagar").statusCode(503).body("mensagem", equalTo("Tesouraria indisponível no momento"));

        given().queryParam("tamanho", 100).when().get("/processos").then().statusCode(200)
                .body("itens.find { it.id == %s }.estado".formatted(processo), equalTo("APROVADO"))
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), nullValue());

        TesourariaFake.falhar = false;
        acao(processo, "pagar").statusCode(200).body("estado", equalTo("PAGO"));
    }
}
