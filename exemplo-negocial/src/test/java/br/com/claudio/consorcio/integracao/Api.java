package br.com.claudio.consorcio.integracao;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Atalhos de RestAssured para os testes de integração. */
final class Api {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    /** Valor padrão do processo: válido (R12) e dentro do crédito de 250.000 das cotas dos testes (R13). */
    static final BigDecimal VALOR_PADRAO = new BigDecimal("30000.00");

    private Api() {
    }

    static String unico(String prefixo) {
        return prefixo + SEQUENCIA.incrementAndGet();
    }

    static long criarCliente(String nome) {
        return post("/clientes", Map.of("nome", nome, "cpf", unico("CPF"))).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static long criarCota(long clienteId) {
        return post("/cotas", Map.of("numero", unico("COTA-"), "valorCredito", 250000, "clienteId", clienteId))
                .statusCode(201).extract().jsonPath().getLong("id");
    }

    static long criarProcesso(long cotaId, String tipo) {
        return post("/processos", corpoDeProcesso(cotaId, tipo)).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    /** Pedido de abertura válido: o valor padrão inteiro para um único favorecido com CPF próprio. */
    static Map<String, Object> corpoDeProcesso(long cotaId, String tipo) {
        return corpoDeProcesso(cotaId, tipo, VALOR_PADRAO,
                List.of(favorecido("Favorecido de Teste", "%011d".formatted(SEQUENCIA.incrementAndGet() % 100_000_000_000L),
                        "vendedor", VALOR_PADRAO)));
    }

    static Map<String, Object> corpoDeProcesso(long cotaId, String tipo, BigDecimal valor, List<Map<String, Object>> favorecidos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("cotaId", cotaId);
        corpo.put("tipo", tipo);
        corpo.put("valor", valor);
        corpo.put("favorecidos", favorecidos);
        return corpo;
    }

    static Map<String, Object> favorecido(String nome, String documento, String papel, BigDecimal valor) {
        Map<String, Object> favorecido = new LinkedHashMap<>();
        favorecido.put("nome", nome);
        favorecido.put("documento", documento);
        favorecido.put("papel", papel);
        favorecido.put("valor", valor);
        return favorecido;
    }

    static ValidatableResponse acao(long processoId, String acao) {
        return given().when().post("/processos/" + processoId + "/" + acao).then();
    }

    /** Ação cujo corpo é obrigatório e traz o motivo. */
    static ValidatableResponse acaoComMotivo(long processoId, String acao, String motivo) {
        return post("/processos/" + processoId + "/" + acao, Map.of("motivo", motivo));
    }

    /** Solicita o pagamento: só grava Pagamento solicitado; a rotina é que fala com a tesouraria. */
    static ValidatableResponse solicitarPagamento(long processoId) {
        return acao(processoId, "solicitar-pagamento");
    }

    /** Estado do processo, lido da última entrada do histórico (ABERTO se ainda não há nenhuma). */
    static String estado(long processoId) {
        List<Map<String, Object>> historico = given().when().get("/processos/" + processoId + "/historico").then()
                .statusCode(200).extract().jsonPath().getList("$");
        return historico.isEmpty() ? "ABERTO" : (String) historico.getLast().get("para");
    }

    static List<Map<String, Object>> historico(long processoId) {
        return given().when().get("/processos/" + processoId + "/historico").then().statusCode(200)
                .extract().jsonPath().getList("$");
    }

    static ValidatableResponse post(String path, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(path).then();
    }
}
