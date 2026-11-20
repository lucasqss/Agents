package br.com.claudio.pagamento.aceitacao;

import com.intuit.karate.junit5.Karate;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;

/**
 * Runner Karate: um teste JUnit por cenário de aceitacao/processo/cenarios.yaml, contra a aplicação no banco da aceitação.
 * <p>{@code Karate.run} executa os cenários em sequência, na ordem do arquivo: é isso que torna os IDs gerados (1000,
 * 1001...) reprodutíveis. Não troque por {@code Runner.parallel}: em paralelo, a ordem de criação (e os IDs) muda a cada
 * build. Esta classe roda numa execução própria do surefire (e o *IT, do failsafe), numa JVM sem nenhum outro teste.
 */
@QuarkusTest
@WithTestResource(BancoDaAceitacao.class)
class ProcessoAceitacaoTest {

    @Karate.Test
    Karate cenarios() {
        return Karate.run("classpath:aceitacao/processo/processo.feature")
                .systemProperty("aceitacao.porta", String.valueOf(RestAssured.port));
    }
}
