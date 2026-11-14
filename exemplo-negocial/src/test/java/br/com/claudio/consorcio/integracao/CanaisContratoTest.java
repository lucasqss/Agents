package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static br.com.claudio.consorcio.integracao.Api.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.path.json.JsonPath;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato de cada canal: nomes exatos dos campos e textos formatados. O front low-code liga os campos
 * direto na tela, então renomear, remover ou mudar um formato tem que quebrar o build.
 */
@QuarkusTest
class CanaisContratoTest {

    private long cliente;
    private long cota;
    private long cotaSemProcesso;

    @BeforeEach
    void criarDados() {
        String cpf = "%011d".formatted(Long.parseLong(Api.unico("")));
        cliente = Api.post("/clientes", Map.of("nome", "Júlia Araújo Lima", "cpf", cpf)).statusCode(201)
                .extract().jsonPath().getLong("id");
        cota = Api.criarCota(cliente);
        cotaSemProcesso = Api.criarCota(cliente);
        long processo = Api.criarProcesso(cota, "IMOBILIARIO");
        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200);
    }

    @Test
    void web() {
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
    void mobile() {
        JsonPath json = get("/mobile/v1/cotas/" + cota);
        assertEquals(Set.of("id", "numero", "valorCredito", "valorCreditoFormatado", "titular", "estado",
                "situacao"), json.getMap("").keySet());
        assertEquals("R$ 250 mil", json.getString("valorCreditoFormatado"));
        assertEquals("Júlia Lima", json.getString("titular"));
        assertEquals("Aguard. vistoria", json.getString("situacao"));

        given().when().get("/mobile/v1/cotas/" + cotaSemProcesso).then().statusCode(200)
                .body("situacao", equalTo("Sem processo"));

        JsonPath clientes = get("/mobile/v1/clientes");
        assertEquals(Set.of("itens", "totalCotas", "totalCotasDescricao"), clientes.getMap("").keySet());
        Map<String, Object> item = clientes.getMap("itens.find { it.id == %s }".formatted(cliente));
        assertEquals(Set.of("id", "nome", "cpf", "quantidadeCotas", "quantidadeCotasDescricao"), item.keySet());
        assertEquals("2 cotas", item.get("quantidadeCotasDescricao"));
        assertThat((String) item.get("cpf"), matchesPattern("\\*{3}\\.\\d{3}\\.\\d{3}-\\*{2}"));
        List<Integer> quantidades = clientes.getList("itens.quantidadeCotas", Integer.class);
        assertEquals(quantidades.stream().mapToLong(Integer::longValue).sum(), clientes.getLong("totalCotas"));
    }

    @Test
    void taa() {
        JsonPath json = get("/taa/v1/cotas/" + cota);
        assertEquals(Set.of("numero", "valorCredito", "valorCreditoFormatado", "titular", "situacao"),
                json.getMap("").keySet());
        assertEquals("R$250.000,00", json.getString("valorCreditoFormatado"));
        assertEquals("JULIA ARAUJO LIMA", json.getString("titular"));
        assertEquals("AGUARDANDO VISTORIA", json.getString("situacao"));
    }

    @Test
    void cotaInexistenteEm404EmTodosOsCanais() {
        for (String canal : List.of("web", "mobile", "taa")) {
            given().when().get("/%s/v1/cotas/999999999".formatted(canal)).then().statusCode(404);
        }
    }

    @Test
    void apiNeutraContinuaSemFormatacao() {
        given().when().get("/cotas/" + cota).then().statusCode(200)
                .body("processoAtualEstado", equalTo("AGUARDANDO_VISTORIA"))
                .body("$", not(hasKey("valorCreditoFormatado")));
    }

    private static JsonPath get(String caminho) {
        return given().when().get(caminho).then().statusCode(200).extract().jsonPath();
    }
}
