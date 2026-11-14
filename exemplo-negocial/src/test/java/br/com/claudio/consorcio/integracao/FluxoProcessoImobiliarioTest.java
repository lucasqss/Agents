package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FluxoProcessoImobiliarioTest {

    @Inject
    ExecutarRotinaDePagamentoUseCase rotina;

    @Test
    void fluxoCompletoComVistoria() {
        long cliente = Api.criarCliente("Ana Imobiliário");
        long cota = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "IMOBILIARIO");

        acao(processo, "iniciar-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("AGUARDANDO_VISTORIA"));

        // Sem vistoria não pode solicitar o pagamento.
        Api.solicitarPagamento(processo).statusCode(409);

        // Leitura: o detalhe da cota mostra o processo atual.
        given().when().get("/cotas/" + cota).then().statusCode(200)
                .body("clienteNome", equalTo("Ana Imobiliário"))
                .body("processoAtualId", equalTo((int) processo))
                .body("processoAtualEstado", equalTo("AGUARDANDO_VISTORIA"));

        acao(processo, "concluir-vistoria").statusCode(200).body("estado", equalTo("APROVADO"));
        Api.solicitarPagamento(processo).statusCode(200).body("estado", equalTo("PAGAMENTO_SOLICITADO"));
        // Em pagamento solicitado nem cancelar vale: só a rotina tira o processo dali.
        Api.acaoComMotivo(processo, "cancelar", "desistência").statusCode(409);
        rotina.executar();
        Api.acaoComMotivo(processo, "cancelar", "desistência").statusCode(409);

        given().queryParam("tipo", "IMOBILIARIO").queryParam("estado", "PAGO").queryParam("tamanho", 100)
                .when().get("/processos").then().statusCode(200)
                .body("itens.id", hasItem((int) processo))
                .body("itens.tipo", everyItem(equalTo("IMOBILIARIO")))
                .body("itens.estado", everyItem(equalTo("PAGO")))
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), startsWith("TES-"))
                .body("total", greaterThanOrEqualTo(1));

        given().when().get("/clientes").then().statusCode(200)
                .body("find { it.id == %s }.quantidadeCotas".formatted(cliente), equalTo(1));

        // Com a cota finalizada, um novo processo pode ser aberto, e ele passa a ser o atual.
        long novo = Api.criarProcesso(cota, "IMOBILIARIO");
        given().when().get("/cotas/" + cota).then()
                .body("processoAtualId", equalTo((int) novo))
                .body("processoAtualEstado", equalTo("ABERTO"));
    }
}
