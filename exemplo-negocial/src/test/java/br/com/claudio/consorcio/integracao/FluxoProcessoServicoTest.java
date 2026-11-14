package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.startsWith;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.Map;
import org.junit.jupiter.api.Test;

@QuarkusTest
class FluxoProcessoServicoTest {

    @Inject
    ExecutarRotinaDePagamentoUseCase rotina;

    @Test
    void fluxoCompletoSemVistoria() {
        long cliente = Api.criarCliente("Bruno Serviço");
        long cota = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "SERVICO");

        // Só um processo ativo por cota.
        Api.post("/processos", Api.corpoDeProcesso(cota, "SERVICO")).statusCode(409);

        acao(processo, "iniciar-analise").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        acao(processo, "concluir-vistoria").statusCode(409);
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("APROVADO"));
        Api.solicitarPagamento(processo).statusCode(200).body("estado", equalTo("PAGAMENTO_SOLICITADO"));
        given().when().get("/cotas/" + cota).then().statusCode(200)
                .body("processoAtualEstado", equalTo("PAGAMENTO_SOLICITADO"));

        // Só a rotina agendada tira o processo dali.
        rotina.executar();

        given().when().get("/cotas/" + cota).then().statusCode(200)
                .body("processoAtualEstado", equalTo("PAGO"))
                .body("processoAtualTipo", equalTo("SERVICO"));

        given().queryParam("tipo", "servico").queryParam("tamanho", 100)
                .when().get("/processos").then().statusCode(200)
                .body("itens.id", hasItem((int) processo))
                .body("itens.tipo", everyItem(equalTo("SERVICO")))
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), startsWith("TES-"));

        given().queryParam("tamanho", 1).when().get("/processos").then().statusCode(200)
                .body("itens.size()", lessThanOrEqualTo(1))
                .body("pagina", equalTo(1));
    }

    @Test
    void cancelaEBloqueiaExclusoesComDependencias() {
        long cliente = Api.criarCliente("Carla");
        long cota = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "SERVICO");

        Api.acaoComMotivo(processo, "cancelar", "desistência da cota").statusCode(200).body("estado", equalTo("CANCELADO"));
        acao(processo, "iniciar-analise").statusCode(409);

        given().when().delete("/clientes/" + cliente).then().statusCode(409);
        given().when().delete("/cotas/" + cota).then().statusCode(409);
        Api.solicitarPagamento(999_999_999L).statusCode(404);
    }

    @Test
    void crudDeClienteECota() {
        String cpf = Api.unico("CPF");
        long cliente = Api.post("/clientes", Map.of("nome", "Duda", "cpf", cpf)).statusCode(201)
                .extract().jsonPath().getLong("id");

        Api.post("/clientes", Map.of("nome", "Outra", "cpf", cpf)).statusCode(409);
        Api.post("/clientes", Map.of("nome", " ", "cpf", Api.unico("CPF"))).statusCode(400);

        given().contentType("application/json").body(Map.of("nome", "Duda Silva", "cpf", cpf))
                .when().put("/clientes/" + cliente).then().statusCode(204);

        long cota = Api.criarCota(cliente);
        Api.post("/cotas", Map.of("numero", Api.unico("COTA-"), "valorCredito", 0, "clienteId", cliente))
                .statusCode(400);

        given().when().get("/clientes").then()
                .body("find { it.id == %s }.nome".formatted(cliente), equalTo("Duda Silva"));

        given().when().delete("/cotas/" + cota).then().statusCode(204);
        given().when().get("/cotas/" + cota).then().statusCode(404);
        given().when().delete("/clientes/" + cliente).then().statusCode(204);
    }
}
