package br.com.claudio.consorcio.canal.web;

import static br.com.claudio.consorcio.canal.web.Api.acao;
import static br.com.claudio.consorcio.canal.web.Api.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FluxoProcessoImobiliarioTest {

    @Test
    void fluxoCompletoComVistoria() {
        long cliente = Api.criarCliente("Ana Imobiliário");
        long cota = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "IMOBILIARIO");

        acao(processo, "iniciar-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("AGUARDANDO_VISTORIA"));

        // Sem vistoria não pode pagar.
        acao(processo, "pagar").statusCode(409);

        // Leitura: o detalhe da cota mostra o processo atual.
        given().when().get("/web/v1/cotas/" + cota).then().statusCode(200)
                .body("clienteNome", equalTo("Ana Imobiliário"))
                .body("processoAtual.id", equalTo((int) processo))
                .body("processoAtual.estado", equalTo("AGUARDANDO_VISTORIA"));

        acao(processo, "concluir-vistoria").statusCode(200).body("estado", equalTo("APROVADO"));
        acao(processo, "pagar").statusCode(200).body("estado", equalTo("PAGO"));
        acao(processo, "cancelar").statusCode(409);

        given().queryParam("tipo", "IMOBILIARIO").queryParam("estado", "PAGO").queryParam("tamanho", 100)
                .when().get("/web/v1/processos").then().statusCode(200)
                .body("itens.id", hasItem((int) processo))
                .body("itens.tipo", everyItem(equalTo("IMOBILIARIO")))
                .body("itens.estado", everyItem(equalTo("PAGO")))
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), startsWith("TES-"))
                .body("total", greaterThanOrEqualTo(1));

        given().when().get("/web/v1/clientes").then().statusCode(200)
                .body("find { it.id == %s }.quantidadeCotas".formatted(cliente), equalTo(1));

        // Com a cota finalizada, um novo processo pode ser aberto, e ele passa a ser o atual.
        long novo = Api.criarProcesso(cota, "IMOBILIARIO");
        given().when().get("/web/v1/cotas/" + cota).then()
                .body("processoAtual.id", equalTo((int) novo))
                .body("processoAtual.estado", equalTo("ABERTO"));
    }
}
