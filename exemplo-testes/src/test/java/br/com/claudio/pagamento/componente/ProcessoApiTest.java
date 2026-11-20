package br.com.claudio.pagamento.componente;

import static br.com.claudio.pagamento.componente.Api.abrir;
import static br.com.claudio.pagamento.componente.Api.abrirOk;
import static br.com.claudio.pagamento.componente.Api.executar;
import static br.com.claudio.pagamento.componente.Api.get;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * O serviço montado, com PostgreSQL de verdade: o estado e o histórico ficam gravados, a entrada fora do formato vira 400
 * com os campos, e cada recusa sai com o status certo e o formato único de erro. As regras em si estão provadas na
 * unidade; aqui, um exemplo de cada passa pela pilha inteira.
 */
@QuarkusTest
class ProcessoApiTest {

    @Test
    void abrirDevolve201ComLocationEGravaAberto() {
        var resposta = abrir("SERVICO", "COTA-77", "1234.50").statusCode(201).extract();
        long id = resposta.jsonPath().getLong("id");
        assertThat(resposta.header("Location"), endsWith("/processos/" + id));
        get("/processos/{id}", id).statusCode(200)
                .body("tipo", equalTo("SERVICO"))
                .body("numeroCota", equalTo("COTA-77"))
                .body("valor", equalTo(1234.5f))
                .body("estado", equalTo("ABERTO"));
        get("/processos/{id}/historico", id).statusCode(200).body("$", empty());
    }

    @Test
    void fluxoDoServicoGravaEstadoEHistorico() {
        long id = abrirOk("SERVICO", "30000.00");
        executar(id, "INICIAR_ANALISE", "ana").statusCode(200).body("estado", equalTo("EM_ANALISE"));
        executar(id, "SOLICITAR_DOCUMENTO", "ana", "falta o RG").statusCode(200).body("estado", equalTo("PENDENTE_DOCUMENTO"));
        executar(id, "RECEBER_DOCUMENTO", "ana").statusCode(200);
        executar(id, "CONCLUIR_ANALISE", "ana").statusCode(200).body("estado", equalTo("APROVADO"));
        executar(id, "PAGAR", "bia").statusCode(200).body("estado", equalTo("PAGO"));

        get("/processos/{id}", id).body("estado", equalTo("PAGO"));
        get("/processos/{id}/acoes", id).body("$", empty());
        get("/processos/{id}/historico", id).statusCode(200)
                .body("acao", contains("INICIAR_ANALISE", "SOLICITAR_DOCUMENTO", "RECEBER_DOCUMENTO", "CONCLUIR_ANALISE", "PAGAR"))
                .body("para", contains("EM_ANALISE", "PENDENTE_DOCUMENTO", "EM_ANALISE", "APROVADO", "PAGO"))
                .body("[1].observacao", equalTo("falta o RG"))
                .body("[0].observacao", nullValue())
                .body("[4].autor", equalTo("bia"))
                .body("[4].quando", notNullValue());
    }

    @Test
    void imobiliarioAcimaDaAlcadaPassaPelaVistoriaEPelaAlcada() {
        long id = abrirOk("IMOBILIARIO", "80000.00");
        executar(id, "INICIAR_ANALISE", "ana");
        executar(id, "CONCLUIR_ANALISE", "ana").body("estado", equalTo("AGUARDANDO_VISTORIA"));
        get("/processos/{id}/acoes", id).body("$", contains("CANCELAR", "CONCLUIR_VISTORIA", "DEVOLVER_PARA_ANALISE"));
        executar(id, "CONCLUIR_VISTORIA", "vitor").body("estado", equalTo("AGUARDANDO_ALCADA"));
        executar(id, "APROVAR_NA_ALCADA", "carla").body("estado", equalTo("APROVADO"));
    }

    @Test
    void transicaoInvalidaE409NoFormatoDeErroENaoGrava() {
        long id = abrirOk("SERVICO", "10.00");
        executar(id, "PAGAR", "ana").statusCode(409)
                .body("mensagem", equalTo("Não é possível pagar um processo no estado ABERTO"))
                .body("campos", empty());
        get("/processos/{id}", id).body("estado", equalTo("ABERTO"));
        get("/processos/{id}/historico", id).body("$", empty());
    }

    @Test
    void motivoObrigatorioE400() {
        long id = abrirOk("SERVICO", "10.00");
        executar(id, "CANCELAR", "ana").statusCode(400)
                .body("mensagem", equalTo("Motivo é obrigatório para cancelar um processo"));
        get("/processos/{id}", id).body("estado", equalTo("ABERTO"));
    }

    @Test
    void entradaForaDoFormatoE400ComOsCampos() {
        RestAssured.given().contentType(ContentType.JSON).body(Map.of("valor", -1)).when().post("/processos").then()
                .statusCode(400)
                .body("mensagem", equalTo("Dados inválidos"))
                .body("campos.campo", hasItems("tipo", "numeroCota", "valor"));
        abrir("SERVICO", "x".repeat(31), "10.00").statusCode(400).body("campos.campo", contains("numeroCota"));
        abrir("SERVICO", "COTA-1", "10.001").statusCode(400).body("campos.campo", contains("valor"));
    }

    @Test
    void tipoDesconhecidoE400NoCampo() {
        RestAssured.given().contentType(ContentType.JSON)
                .body(Map.of("tipo", "VEICULO", "numeroCota", "COTA-1", "valor", 10)).when().post("/processos").then()
                .statusCode(400).body("campos.campo", contains("tipo"));
    }

    @Test
    void semUsuarioE400() {
        long id = abrirOk("SERVICO", "10.00");
        RestAssured.given().when().post("/processos/{id}/acoes/INICIAR_ANALISE", id).then()
                .statusCode(400).body("campos.campo", hasItem("usuario"));
    }

    @Test
    void inexistenteE404() {
        get("/processos/{id}", 999_999).statusCode(404).body("mensagem", equalTo("Processo 999999 não encontrado(a)"));
        get("/processos/{id}/acoes", 999_999).statusCode(404);
        get("/processos/{id}/historico", 999_999).statusCode(404);
        executar(999_999, "INICIAR_ANALISE", "ana").statusCode(404);
    }

    @Test
    void acaoDesconhecidaE404NoFormatoDeErro() {
        long id = abrirOk("SERVICO", "10.00");
        executar(id, "APROVAR_TUDO", "ana").statusCode(404).body("mensagem", equalTo("Recurso não encontrado"));
    }
}
