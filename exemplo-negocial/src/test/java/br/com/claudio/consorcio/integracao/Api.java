package br.com.claudio.consorcio.integracao;

import static io.restassured.RestAssured.given;

import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
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
        return post("/processos", Map.of("cotaId", cotaId, "tipo", tipo, "valor", VALOR_PADRAO)).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static ValidatableResponse acao(long processoId, String acao) {
        return given().when().post("/processos/" + processoId + "/" + acao).then();
    }

    /** Ação cujo corpo é obrigatório e traz o motivo. */
    static ValidatableResponse acaoComMotivo(long processoId, String acao, String motivo) {
        return post("/processos/" + processoId + "/" + acao, Map.of("motivo", motivo));
    }

    static ValidatableResponse post(String path, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(path).then();
    }
}
