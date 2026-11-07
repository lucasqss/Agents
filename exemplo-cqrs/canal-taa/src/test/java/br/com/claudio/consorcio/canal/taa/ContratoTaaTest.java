package br.com.claudio.consorcio.canal.taa;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Contrato do terminal: campos e formatos, token do TAA e escrita bloqueada. */
@QuarkusTest
class ContratoTaaTest {

    /** Token fictício do terminal (veja MapeadorDeTokenTaa). */
    private static final String TOKEN = "Bearer taa-T0042";

    @Inject
    MassaDeDados massa;

    private long cota;

    @BeforeEach
    void criarDados() {
        cota = massa.cota(massa.cliente("Júlia Araújo Lima"));
        massa.processoAguardandoVistoria(cota);
    }

    @Test
    void cota() {
        JsonPath json = comToken().when().get("/taa/v1/cotas/" + cota).then().statusCode(200).extract().jsonPath();
        assertEquals(Set.of("numero", "valorCredito", "valorCreditoFormatado", "titular", "situacao"),
                json.getMap("").keySet());
        assertEquals("R$250.000,00", json.getString("valorCreditoFormatado"));
        assertEquals("JULIA ARAUJO LIMA", json.getString("titular"));
        assertEquals("AGUARDANDO VISTORIA", json.getString("situacao"));
    }

    @Test
    void escritaBloqueada() {
        // O terminal não expõe nenhuma escrita; CAN-010 garante que nem o código do canal usa o command.
        comToken().when().post("/taa/v1/cotas/" + cota).then().statusCode(405);
        comToken().when().delete("/taa/v1/cotas/" + cota).then().statusCode(405);
    }

    @Test
    void semTokenDoTaaEm401() {
        RestAssured.given().when().get("/taa/v1/cotas/" + cota).then().statusCode(401);
    }

    @Test
    void cotaInexistenteEm404() {
        comToken().when().get("/taa/v1/cotas/999999999").then().statusCode(404);
    }

    private static RequestSpecification comToken() {
        return RestAssured.given().header("Authorization", TOKEN);
    }
}
