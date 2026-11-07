package br.com.claudio.consorcio.canal.mobile;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Contrato do app: nomes exatos dos campos e textos formatados (o front low-code liga os campos direto na
 * tela), o token do mobile e a regra do canal na escrita.
 */
@QuarkusTest
class ContratoMobileTest {

    /** Token fictício do mobile (veja MapeadorDeTokenMobile). */
    private static final String TOKEN = "Bearer mob.12345678909";

    @Inject
    MassaDeDados massa;

    private long cliente;
    private long cota;
    private long cotaSemProcesso;

    @BeforeEach
    void criarDados() {
        cliente = massa.cliente("Júlia Araújo Lima");
        cota = massa.cota(cliente);
        cotaSemProcesso = massa.cota(cliente);
        massa.processoAguardandoVistoria(cota);
    }

    @Test
    void cota() {
        JsonPath json = get("/mobile/v1/cotas/" + cota);
        assertEquals(Set.of("id", "numero", "valorCredito", "valorCreditoFormatado", "titular", "estado",
                "situacao"), json.getMap("").keySet());
        assertEquals("R$ 250 mil", json.getString("valorCreditoFormatado"));
        assertEquals("Júlia Lima", json.getString("titular"));
        assertEquals("Aguard. vistoria", json.getString("situacao"));

        assertEquals("Sem processo", get("/mobile/v1/cotas/" + cotaSemProcesso).getString("situacao"));
    }

    @Test
    void clientesComTotalSomado() {
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
    void appAbreProcessoDeServico() {
        comToken().contentType(ContentType.JSON).body(Map.of("cotaId", cotaSemProcesso, "tipo", "SERVICO"))
                .when().post("/mobile/v1/processos").then().statusCode(201);
        assertEquals("Aberto", get("/mobile/v1/cotas/" + cotaSemProcesso).getString("situacao"));
    }

    @Test
    void appNaoAbreProcessoImobiliario() {
        // Mesmo fluxo do web, validação divergente: a regra é do domínio (CanalDeAtendimento.MOBILE).
        comToken().contentType(ContentType.JSON).body(Map.of("cotaId", cotaSemProcesso, "tipo", "IMOBILIARIO"))
                .when().post("/mobile/v1/processos").then().statusCode(400)
                .body("mensagem", equalTo("O canal MOBILE não abre processo do tipo IMOBILIARIO"));
    }

    @Test
    void formatoDaEntradaNoRequestDoCanal() {
        comToken().contentType(ContentType.JSON).body(Map.of("tipo", "SERVICO"))
                .when().post("/mobile/v1/processos").then().statusCode(400)
                .body("campos.campo", hasItem("cotaId"));
    }

    @Test
    void semTokenDoMobileEm401() {
        RestAssured.given().when().get("/mobile/v1/cotas/" + cota).then().statusCode(401);
        RestAssured.given().header("Authorization", "Bearer web:M123")
                .when().get("/mobile/v1/cotas/" + cota).then().statusCode(401);
    }

    @Test
    void cotaInexistenteEm404() {
        comToken().when().get("/mobile/v1/cotas/999999999").then().statusCode(404);
    }

    private static RequestSpecification comToken() {
        return RestAssured.given().header("Authorization", TOKEN);
    }

    private static JsonPath get(String caminho) {
        return comToken().when().get(caminho).then().statusCode(200).extract().jsonPath();
    }
}
