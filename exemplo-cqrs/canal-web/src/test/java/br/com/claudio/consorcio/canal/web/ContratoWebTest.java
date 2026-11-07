package br.com.claudio.consorcio.canal.web;

import static br.com.claudio.consorcio.canal.web.Api.acao;
import static br.com.claudio.consorcio.canal.web.Api.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato do canal web: nomes exatos dos campos e textos formatados. O front low-code liga os campos
 * direto na tela, então renomear, remover ou mudar um formato tem que quebrar o build.
 */
@QuarkusTest
class ContratoWebTest {

    private long cliente;
    private long cota;
    private long cotaSemProcesso;

    @BeforeEach
    void criarDados() {
        String cpf = "%011d".formatted(Long.parseLong(Api.unico("")));
        cliente = Api.post("/web/v1/clientes", Map.of("nome", "Júlia Araújo Lima", "cpf", cpf)).statusCode(201)
                .extract().jsonPath().getLong("id");
        cota = Api.criarCota(cliente);
        cotaSemProcesso = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "IMOBILIARIO");
        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200);
    }

    @Test
    void cota() {
        JsonPath json = get("/web/v1/cotas/" + cota);
        assertEquals(Set.of("id", "numero", "valorCredito", "valorCreditoFormatado", "clienteId", "clienteNome",
                "processoAtual"), json.getMap("").keySet());
        assertEquals(Set.of("id", "tipo", "tipoDescricao", "estado", "estadoDescricao"),
                json.getMap("processoAtual").keySet());
        assertEquals("R$ 250.000,00", json.getString("valorCreditoFormatado"));
        assertEquals("Júlia Araújo Lima", json.getString("clienteNome"));
        assertEquals("Imobiliário", json.getString("processoAtual.tipoDescricao"));
        assertEquals("AGUARDANDO_VISTORIA", json.getString("processoAtual.estado"));
        assertEquals("Aguardando vistoria", json.getString("processoAtual.estadoDescricao"));

        given().when().get("/web/v1/cotas/" + cotaSemProcesso).then().statusCode(200)
                .body("processoAtual", nullValue());
    }

    @Test
    void clientes() {
        Map<String, Object> item = get("/web/v1/clientes").getMap("find { it.id == %s }".formatted(cliente));
        assertEquals(Set.of("id", "nome", "cpf", "cpfFormatado", "quantidadeCotas", "quantidadeCotasDescricao"),
                item.keySet());
        assertEquals("2 cotas", item.get("quantidadeCotasDescricao"));
    }

    @Test
    void processos() {
        JsonPath json = get("/web/v1/processos?tamanho=100");
        assertEquals(Set.of("itens", "pagina", "tamanho", "total"), json.getMap("").keySet());
        Map<String, Object> item = json.getMap("itens.find { it.cotaId == %s }".formatted(cota));
        assertEquals(Set.of("id", "cotaId", "cotaNumero", "tipo", "tipoDescricao", "estado", "estadoDescricao",
                "protocoloPagamento"), item.keySet());
        assertEquals("Aguardando vistoria", item.get("estadoDescricao"));
    }

    @Test
    void cotaInexistenteEm404() {
        given().when().get("/web/v1/cotas/999999999").then().statusCode(404);
    }

    @Test
    void semTokenDoWebEm401() {
        RestAssured.given().when().get("/web/v1/cotas/" + cota).then().statusCode(401)
                .body("mensagem", equalTo("Token do canal ausente ou inválido"));
        RestAssured.given().header("Authorization", "Bearer mob.12345678909")
                .when().get("/web/v1/cotas/" + cota).then().statusCode(401);
    }

    private static JsonPath get(String caminho) {
        return given().when().get(caminho).then().statusCode(200).extract().jsonPath();
    }
}
