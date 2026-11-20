package cucumber;

import static org.junit.jupiter.api.Assertions.fail;

import io.cucumber.core.cli.Main;
import io.cucumber.plugin.event.Status;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Runner da aceitação: executa as especificações em português de {@code src/test/resources/cucumber/features/} com o Cucumber,
 * contra a aplicação no banco da aceitação, e devolve um teste JUnit por cenário (o nome traz o arquivo e o cenário).
 *
 * <ul>
 * <li>Os arquivos rodam em ordem alfabética ({@code --order lexical}), um cenário de cada vez, na ordem do arquivo: é isso
 * que torna os IDs gerados (1000, 1001...) reprodutíveis. Não ligue execução paralela.</li>
 * <li>Antes de tudo, a sentinela: o processo 1 da massa tem de estar lá como foi gravado, senão a aplicação não subiu
 * sobre o banco da aceitação e nenhum ID esperado vale.</li>
 * <li>Esta classe roda numa execução própria do surefire (e o *IT, do failsafe), numa JVM sem nenhum outro teste.</li>
 * </ul>
 */
@QuarkusTest
@WithTestResource(BancoDaAceitacao.class)
class ProcessoAceitacaoTest {

    private static final String PASSOS = "cucumber.passos";

    @TestFactory
    Stream<DynamicTest> cenarios() {
        conferirSentinela();
        Resultados.limpar();
        Main.run(new String[] {
            "--glue", PASSOS,
            "--plugin", Resultados.class.getName(),
            "--plugin", "pretty",
            "--plugin", "html:target/cucumber/" + getClass().getSimpleName() + ".html",
            "--order", "lexical",
            "--monochrome",
            "classpath:cucumber/features"}, getClass().getClassLoader());
        List<Resultados.Cenario> cenarios = Resultados.todos();
        if (cenarios.isEmpty()) {
            fail("Nenhum cenário executado em src/test/resources/cucumber/features/");
        }
        return cenarios.stream().map(c -> DynamicTest.dynamicTest(c.nome(), () -> {
            if (c.status() != Status.PASSED) {
                fail(c.nome() + ": " + (c.erro() == null ? c.status() + " (passo sem definição ou pulado)" : c.erro().getMessage()),
                        c.erro());
            }
        }));
    }

    private static void conferirSentinela() {
        Response resposta = RestAssured.given().when().get("/processos/1");
        JsonPath corpo = resposta.jsonPath();
        boolean sentinela = resposta.statusCode() == 200 && "COTA-SENTINELA".equals(corpo.getString("numeroCota"))
                && "ABERTO".equals(corpo.getString("estado"));
        if (!sentinela) {
            fail("A aplicação não está sobre o banco da aceitação (BancoDaAceitacao): o processo 1 da massa não foi"
                    + " encontrado como gravado. Resposta: " + resposta.statusCode() + " " + resposta.asString());
        }
    }
}
