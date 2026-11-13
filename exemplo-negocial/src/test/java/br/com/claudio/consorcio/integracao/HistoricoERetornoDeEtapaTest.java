package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HistoricoERetornoDeEtapaTest {

    @Test
    void imobiliarioDevolveDaVistoriaEDeAprovadoERegistraTudoNoHistorico() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Hugo")), "IMOBILIARIO");
        acoes(processo).body("acoes", contains("iniciar-analise", "cancelar"));
        historico(processo).body("$", empty());

        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("AGUARDANDO_VISTORIA"));
        acoes(processo).body("acoes", contains("concluir-vistoria", "devolver-para-analise", "cancelar"));

        comObservacao(processo, "devolver-para-analise", "Matrícula ilegível")
                .statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200);
        acao(processo, "concluir-vistoria").statusCode(200).body("estado", equalTo("APROVADO"));
        acoes(processo).body("acoes", contains("devolver-para-analise", "pagar", "cancelar"));

        acao(processo, "devolver-para-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200);
        acao(processo, "concluir-vistoria").statusCode(200);
        acao(processo, "pagar").statusCode(200).body("estado", equalTo("PAGO"));
        acoes(processo).body("acoes", empty());

        historico(processo)
                .body("acao", contains("INICIAR_ANALISE", "CONCLUIR_ANALISE", "DEVOLVER_PARA_ANALISE",
                        "CONCLUIR_ANALISE", "CONCLUIR_VISTORIA", "DEVOLVER_PARA_ANALISE", "CONCLUIR_ANALISE",
                        "CONCLUIR_VISTORIA", "PAGAR"))
                .body("[0].de", equalTo("ABERTO"))
                .body("[0].para", equalTo("EM_ANALISE"))
                .body("[0].observacao", nullValue())
                .body("[2].de", equalTo("AGUARDANDO_VISTORIA"))
                .body("[2].para", equalTo("EM_ANALISE"))
                .body("[2].observacao", equalTo("Matrícula ilegível"))
                .body("[8].para", equalTo("PAGO"))
                .body("quando", everyItem(notNullValue()));
    }

    @Test
    void servicoSoDevolveDepoisDeAprovado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Iara")), "SERVICO");
        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "devolver-para-analise").statusCode(409);

        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("APROVADO"));
        acao(processo, "devolver-para-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
    }

    @Test
    void acaoRecusadaNaoEntraNoHistorico() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("João")), "SERVICO");
        acao(processo, "pagar").statusCode(409);
        comObservacao(processo, "cancelar", "x".repeat(501)).statusCode(400);

        historico(processo).body("$", empty());
    }

    @Test
    void processoInexistente() {
        acoes(999_999_999L, 404);
        given().when().get("/processos/999999999/historico").then().statusCode(404);
    }

    private static ValidatableResponse acoes(long processo) {
        return acoes(processo, 200);
    }

    private static ValidatableResponse acoes(long processo, int status) {
        return given().when().get("/processos/" + processo + "/acoes").then().statusCode(status);
    }

    private static ValidatableResponse historico(long processo) {
        return given().when().get("/processos/" + processo + "/historico").then().statusCode(200);
    }

    private static ValidatableResponse comObservacao(long processo, String acao, String observacao) {
        return given().contentType(ContentType.JSON).body(Map.of("observacao", observacao))
                .when().post("/processos/" + processo + "/" + acao).then();
    }

    @Test
    void corpoQueNaoEJsonEhRecusadoComFormatoPadraoESemMudarOEstado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Marta")), "SERVICO");

        given().contentType(ContentType.TEXT).body("qualquer coisa")
                .when().post("/processos/" + processo + "/iniciar-analise").then()
                .statusCode(415)
                .body("mensagem", equalTo("Tipo de conteúdo não suportado"));

        acoes(processo).body("acoes", contains("iniciar-analise", "cancelar"));
        historico(processo).body("$", empty());
    }
}
