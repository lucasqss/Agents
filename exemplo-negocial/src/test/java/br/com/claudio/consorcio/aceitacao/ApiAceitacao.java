package br.com.claudio.consorcio.aceitacao;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.hamcrest.Matcher;

/**
 * Padrão: Test Utility Method [xUnit Test Patterns].
 * Fala com a aplicação somente por HTTP, usando os caminhos e campos do contrato publicado.
 */
public final class ApiAceitacao {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    private ApiAceitacao() {
    }

    /** Só para as pré-condições de cadastro de cliente e cota, cujo código exato de sucesso ainda não foi definido. */
    public static Matcher<Integer> sucesso() {
        return allOf(greaterThanOrEqualTo(200), lessThan(300));
    }

    public static long novaCota() {
        long cliente = enviar("/clientes", Map.of("nome", "Cliente de Aceitação", "cpf", "%011d".formatted(unico())))
                .statusCode(sucesso()).extract().jsonPath().getLong("id");
        return enviar("/cotas", Map.of("numero", "COTA-" + unico(), "valorCredito", 250000, "clienteId", cliente))
                .statusCode(sucesso()).extract().jsonPath().getLong("id");
    }

    public static long abrirProcesso(long cota, String tipo) {
        return enviar("/processos", Map.of("cotaId", cota, "tipo", tipo))
                .statusCode(201).extract().jsonPath().getLong("id");
    }

    public static ValidatableResponse enviar(String caminho, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(caminho).then();
    }

    public static ValidatableResponse executar(long processo, String acao) {
        return given().when().post("/processos/%d/%s".formatted(processo, acao)).then();
    }

    /** O contrato não tem consulta de processo por id: procura na listagem completa. */
    public static Map<String, Object> consultarProcesso(long processo) {
        return listarProcessos().stream()
                .filter(item -> numero(item, "id") == processo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Processo %d não aparece na listagem".formatted(processo)));
    }

    public static long quantidadeDeProcessosDaCota(long cota) {
        return listarProcessos().stream().filter(item -> numero(item, "cotaId") == cota).count();
    }

    private static List<Map<String, Object>> listarProcessos() {
        List<Map<String, Object>> todos = new ArrayList<>();
        for (int pagina = 1;; pagina++) {
            JsonPath resposta = given().queryParam("pagina", pagina).queryParam("tamanho", 100)
                    .when().get("/processos").then().statusCode(200).extract().jsonPath();
            List<Map<String, Object>> itens = resposta.getList("itens");
            if (itens.isEmpty()) {
                return todos;
            }
            todos.addAll(itens);
        }
    }

    private static long numero(Map<String, Object> item, String campo) {
        return ((Number) item.get(campo)).longValue();
    }

    private static long unico() {
        return SEQUENCIA.incrementAndGet();
    }
}
