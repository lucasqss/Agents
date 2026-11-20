package br.com.claudio.pagamento.aceitacao;

import com.intuit.karate.junit5.Karate;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/** Runner Karate: um teste JUnit por cenário de aceitacao/processo/cenarios.yaml, contra a aplicação no banco da aceitação. */
@QuarkusTest
@WithTestResource(BancoDaAceitacao.class)
class ProcessoAceitacaoTest {

    @Karate.Test
    Karate cenarios() {
        return Karate.run("classpath:aceitacao/processo/processo.feature")
                .systemProperty("aceitacao.porta", String.valueOf(RestAssured.port));
    }
}
