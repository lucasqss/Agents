package PACOTE_RAIZ.aceitacao;

import com.intuit.karate.junit5.Karate;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

@QuarkusTest
@WithTestResource(AntifraudeSimulado.class)
class PedidosAceitacaoTest {

    @Karate.Test
    Karate cenarios() {
        return Karate.run("classpath:aceitacao/pedidos/pedidos.feature")
                .systemProperty("aceitacao.porta", String.valueOf(RestAssured.port));
    }
}
