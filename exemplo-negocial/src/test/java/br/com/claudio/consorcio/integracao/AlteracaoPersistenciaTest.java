package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * A alteração pendente sobrevive a gravar e buscar, e responder a ela troca (ou mantém) os favorecidos gravados.
 * Cada passo grava e relê o processo do banco, então a conferência é do que ficou gravado.
 */
@QuarkusTest
class AlteracaoPersistenciaTest {

    private static final BigDecimal VALOR = new BigDecimal("1000.00");

    private long processoEmAnalise() {
        long cota = Api.criarCota(Api.criarCliente("Cliente Alteração"));
        long processo = Api.post("/processos", Api.corpoDeProcesso(cota, "SERVICO", VALOR, List.of(
                Api.favorecido("Vendedor", "12345678901", "vendedor", new BigDecimal("600.00")),
                Api.favorecido("Cartório", "12345678000199", "cartório", new BigDecimal("400.00")))))
                .statusCode(201).extract().jsonPath().getLong("id");
        Api.acao(processo, "iniciar-analise").statusCode(200);
        return processo;
    }

    private static Map<String, Object> proposta(String motivo) {
        return Map.of("valor", new BigDecimal("700.00"), "motivo", motivo, "favorecidos", List.of(
                Api.favorecido("Novo Vendedor", "98765432100", "vendedor", new BigDecimal("700.00"))));
    }

    @Test
    void propostaSobreviveASalvarEBuscarEOsVigentesNaoMudam() {
        long processo = processoEmAnalise();

        Api.post("/processos/" + processo + "/propor-alteracao", proposta("valor errado"))
                .statusCode(200).body("estado", equalTo("AGUARDANDO_CONCORDANCIA"));

        given().get("/processos/" + processo + "/alteracao-proposta").then().statusCode(200)
                .body("valor", equalTo(700.0f))
                .body("motivo", equalTo("valor errado"))
                .body("favorecidos.nome", contains("Novo Vendedor"))
                .body("favorecidos.documento", contains("98765432100"));
        given().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Vendedor", "Cartório"));
    }

    @Test
    void concordarTrocaOsFavorecidosGravadosEApagaAProposta() {
        long processo = processoEmAnalise();
        Api.post("/processos/" + processo + "/propor-alteracao", proposta("valor errado")).statusCode(200);

        Api.acao(processo, "concordar").statusCode(200).body("estado", equalTo("EM_ANALISE"));

        given().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Novo Vendedor"))
                .body("valor", contains(700.0f));
        given().get("/processos/" + processo + "/alteracao-proposta").then().statusCode(404);
    }

    @Test
    void discordarMantemOsFavorecidosEApagaAProposta() {
        long processo = processoEmAnalise();
        Api.post("/processos/" + processo + "/propor-alteracao", proposta("valor errado")).statusCode(200);

        Api.acaoComMotivo(processo, "discordar", "não concordo").statusCode(200);

        given().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Vendedor", "Cartório"))
                .body("valor", contains(600.0f, 400.0f));
        given().get("/processos/" + processo + "/alteracao-proposta").then().statusCode(404);
    }

    @Test
    void propostaPodeRepetirODocumentoDeUmVigenteEPodeSerRefeitaDepoisDeDiscordar() {
        long processo = processoEmAnalise();
        var repetida = Map.of("valor", VALOR, "motivo", "mesmo documento", "favorecidos", List.of(
                Api.favorecido("Vendedor Novo Nome", "12345678901", "vendedor", VALOR)));
        Api.post("/processos/" + processo + "/propor-alteracao", repetida).statusCode(200);
        Api.acaoComMotivo(processo, "discordar", "não").statusCode(200);
        Api.post("/processos/" + processo + "/propor-alteracao", proposta("outra")).statusCode(200);

        given().get("/processos/" + processo + "/alteracao-proposta").then().statusCode(200)
                .body("motivo", equalTo("outra"))
                .body("favorecidos.documento", contains("98765432100"));
    }

    @Test
    void alterarEmAbertoTrocaOsFavorecidosGravadosRespeitandoOsUnique() {
        long cota = Api.criarCota(Api.criarCliente("Cliente Alterar"));
        long processo = Api.criarProcesso(cota, "SERVICO");

        // A ordem 1 e 2 já estão ocupadas pelos favorecidos da abertura: apagar antes de regravar evita o conflito do unique.
        Api.post("/processos/" + processo + "/alterar", Map.of("valor", VALOR, "favorecidos", List.of(
                Api.favorecido("Primeiro", "12345678901", "vendedor", new BigDecimal("300.00")),
                Api.favorecido("Segundo", "12345678000199", "cartório", new BigDecimal("700.00")))))
                .statusCode(200).body("estado", equalTo("ABERTO"));

        given().get("/processos/" + processo + "/favorecidos").then().statusCode(200)
                .body("nome", contains("Primeiro", "Segundo"))
                .body("valor", contains(300.0f, 700.0f));
        given().get("/processos/" + processo + "/historico").then().statusCode(200)
                .body("acao", contains("ALTERAR"))
                .body("valorAnterior", contains(30000.0f))
                .body("valorNovo", contains(1000.0f));
    }

    @Test
    void historicoGravaOsValoresSoNasAcoesQueAlteramOValor() {
        long processo = processoEmAnalise();
        Api.post("/processos/" + processo + "/propor-alteracao", proposta("valor errado")).statusCode(200);
        Api.acao(processo, "concordar").statusCode(200);

        given().get("/processos/" + processo + "/historico").then().statusCode(200)
                .body("acao", contains("INICIAR_ANALISE", "PROPOR_ALTERACAO", "CONCORDAR"))
                .body("valorAnterior", contains(nullValue(), equalTo(1000.0f), nullValue()))
                .body("valorNovo", contains(nullValue(), equalTo(700.0f), nullValue()))
                .body("observacao", contains(nullValue(), equalTo("valor errado"), nullValue()));
    }
}
