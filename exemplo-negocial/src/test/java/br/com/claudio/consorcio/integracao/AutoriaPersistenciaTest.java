package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste;
import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Fatia 7 contra o banco real: o autor de cada transição e o usuário da última conclusão de análise (R17) são gravados
 * e lidos de volta pela migration V9; as recusas de identidade saem no formato de erro da API (ADR 003).
 */
@QuarkusTest
class AutoriaPersistenciaTest {

    private static final BigDecimal ACIMA_DA_ALCADA = new BigDecimal("80000.00");

    private static long processoEmAguardandoAlcadaConcluidoPorAri() {
        long cota = Api.criarCota(Api.criarCliente("Cliente Autoria"));
        long processo = Api.post("/processos", Api.corpoDeProcesso(cota, "SERVICO", ACIMA_DA_ALCADA,
                List.of(Api.favorecido("Vendedor", "12345678901", "vendedor", ACIMA_DA_ALCADA)))).statusCode(201)
                .extract().jsonPath().getLong("id");
        Api.acao(processo, "iniciar-analise").statusCode(200);
        given().header("Authorization", IdentidadeTeste.ARI.autorizacao()).when()
                .post("/processos/" + processo + "/concluir-analise").then().statusCode(200)
                .body("estado", equalTo("AGUARDANDO_ALCADA"));
        return processo;
    }

    @Test
    void oAutorDeCadaTransicaoFicaNoHistoricoELidoDeVolta() {
        long processo = processoEmAguardandoAlcadaConcluidoPorAri();
        Api.acao(processo, "aprovar-na-alcada").statusCode(200);

        given().when().get("/processos/" + processo + "/historico").then().statusCode(200)
                .body("autor", contains("ana", "ari", "bruno"));
    }

    @Test
    void quemConcluiuAAnaliseNaoAprovaMesmoDepoisDeOProcessoVoltarDoBanco() {
        long processo = processoEmAguardandoAlcadaConcluidoPorAri();
        // Pessoa com o papel de aprovador e o mesmo usuário de quem concluiu a análise.
        String ariAprovador = IdentidadeTeste.Pessoa.de("ari", IdentidadeTeste.BACKOFFICE, IdentidadeTeste.APROVADOR)
                .autorizacao();

        given().header("Authorization", ariAprovador).when().post("/processos/" + processo + "/aprovar-na-alcada").then()
                .statusCode(403).body("mensagem", notNullValue());

        given().when().get("/processos/" + processo + "/historico").then().statusCode(200)
                .body("autor", contains("ana", "ari"));
        Api.acao(processo, "aprovar-na-alcada").statusCode(200).body("estado", equalTo("APROVADO"));
    }

    @Test
    void semTokenOuComTokenInvalidoE401NoFormatoDeErroDaApi() {
        io.restassured.RestAssured.given().when().get("/processos").then().statusCode(401)
                .body("mensagem", notNullValue());
        io.restassured.RestAssured.given().header("Authorization", IdentidadeTeste.autorizacaoMalformada()).when()
                .get("/processos").then().statusCode(401).body("mensagem", notNullValue());
    }

    @Test
    void tokenSemOPapelEh403NoFormatoDeErroDaApi() {
        long cota = Api.criarCota(Api.criarCliente("Cliente Papel"));

        given().header("Authorization", IdentidadeTeste.TEREZA.autorizacao()).contentType("application/json")
                .body(Map.of("cotaId", cota)).when().post("/processos").then().statusCode(403)
                .body("mensagem", notNullValue());
    }
}
