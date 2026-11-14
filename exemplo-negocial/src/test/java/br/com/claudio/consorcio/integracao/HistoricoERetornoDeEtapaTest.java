package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class HistoricoERetornoDeEtapaTest {

    @Inject
    ExecutarRotinaDePagamentoUseCase rotina;

    @Test
    void imobiliarioDevolveDaVistoriaEDeAprovadoERegistraTudoNoHistorico() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Hugo")), "IMOBILIARIO");
        acoes(processo).body("acoes", contains("iniciar-analise", "cancelar", "alterar"));
        historico(processo).body("$", empty());

        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("AGUARDANDO_VISTORIA"));
        acoes(processo).body("acoes", contains("concluir-vistoria", "devolver-para-analise", "cancelar"));

        Api.acaoComMotivo(processo, "devolver-para-analise", "Matrícula ilegível")
                .statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200);
        acao(processo, "concluir-vistoria").statusCode(200).body("estado", equalTo("APROVADO"));
        acoes(processo).body("acoes", contains("devolver-para-analise", "solicitar-pagamento", "cancelar"));

        Api.acaoComMotivo(processo, "devolver-para-analise", "Documento divergente").statusCode(200)
                .body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200);
        acao(processo, "concluir-vistoria").statusCode(200);
        Api.solicitarPagamento(processo).statusCode(200).body("estado", equalTo("PAGAMENTO_SOLICITADO"));
        acoes(processo).body("acoes", empty());
        rotina.executar();
        acoes(processo).body("acoes", empty());

        historico(processo)
                .body("acao", contains("INICIAR_ANALISE", "CONCLUIR_ANALISE", "DEVOLVER_PARA_ANALISE",
                        "CONCLUIR_ANALISE", "CONCLUIR_VISTORIA", "DEVOLVER_PARA_ANALISE", "CONCLUIR_ANALISE",
                        "CONCLUIR_VISTORIA", "SOLICITAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"))
                .body("[0].de", equalTo("ABERTO"))
                .body("[0].para", equalTo("EM_ANALISE"))
                .body("[0].observacao", nullValue())
                .body("[2].de", equalTo("AGUARDANDO_VISTORIA"))
                .body("[2].para", equalTo("EM_ANALISE"))
                .body("[2].observacao", equalTo("Matrícula ilegível"))
                .body("[8].de", equalTo("APROVADO"))
                .body("[8].para", equalTo("PAGAMENTO_SOLICITADO"))
                .body("[9].de", equalTo("PAGAMENTO_SOLICITADO"))
                .body("[9].para", equalTo("PAGO"))
                .body("quando", everyItem(notNullValue()));
    }

    @Test
    void servicoSoDevolveDepoisDeAprovado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Iara")), "SERVICO");
        acao(processo, "iniciar-analise").statusCode(200);
        Api.acaoComMotivo(processo, "devolver-para-analise", "Falta documento").statusCode(409);

        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("APROVADO"));
        Api.acaoComMotivo(processo, "devolver-para-analise", "Falta documento").statusCode(200)
                .body("estado", equalTo("EM_ANALISE"));
    }

    @Test
    void documentoSolicitadoERecebidoFicaNoHistoricoComOMotivo() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Lia")), "SERVICO");
        acao(processo, "iniciar-analise").statusCode(200);

        Api.acaoComMotivo(processo, "solicitar-documento", "Certidão vencida").statusCode(200)
                .body("estado", equalTo("PENDENTE_DOCUMENTO"));
        acoes(processo).body("acoes", contains("receber-documento", "cancelar"));
        acao(processo, "concluir-analise").statusCode(409);

        acao(processo, "receber-documento").statusCode(200).body("estado", equalTo("EM_ANALISE"));

        historico(processo)
                .body("acao", contains("INICIAR_ANALISE", "SOLICITAR_DOCUMENTO", "RECEBER_DOCUMENTO"))
                .body("[1].para", equalTo("PENDENTE_DOCUMENTO"))
                .body("[1].observacao", equalTo("Certidão vencida"))
                .body("[2].observacao", nullValue());
    }

    @Test
    void reprovadoEFinalELiberaACota() {
        long cota = Api.criarCota(Api.criarCliente("Davi"));
        long processo = Api.criarProcesso(cota, "IMOBILIARIO");
        acao(processo, "iniciar-analise").statusCode(200);

        Api.acaoComMotivo(processo, "reprovar", "Favorecido divergente").statusCode(200)
                .body("estado", equalTo("REPROVADO"));

        acoes(processo).body("acoes", empty());
        Api.acaoComMotivo(processo, "cancelar", "desistência").statusCode(409);
        historico(processo).body("[1].acao", equalTo("REPROVAR"))
                .body("[1].observacao", equalTo("Favorecido divergente"));
        given().queryParam("estado", "REPROVADO").queryParam("tamanho", 100).when().get("/processos").then()
                .statusCode(200).body("itens.id", org.hamcrest.Matchers.hasItem((int) processo));
        Api.criarProcesso(cota, "SERVICO");
    }

    @Test
    void acaoRecusadaNaoEntraNoHistorico() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("João")), "SERVICO");
        Api.solicitarPagamento(processo).statusCode(409);
        Api.acaoComMotivo(processo, "cancelar", "x".repeat(501)).statusCode(400);
        acao(processo, "cancelar").statusCode(400);

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


    @Test
    void corpoQueNaoEJsonEhRecusadoComFormatoPadraoESemMudarOEstado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Marta")), "SERVICO");

        given().contentType(ContentType.TEXT).body("qualquer coisa")
                .when().post("/processos/" + processo + "/iniciar-analise").then()
                .statusCode(415)
                .body("mensagem", equalTo("Tipo de conteúdo não suportado"));

        acoes(processo).body("acoes", contains("iniciar-analise", "cancelar", "alterar"));
        historico(processo).body("$", empty());
    }
}
