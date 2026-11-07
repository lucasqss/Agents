package br.com.claudio.consorcio.canal.web;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** Atalhos de RestAssured para os testes do canal web; toda requisição leva o token do web. */
final class Api {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    /** Token fictício do web (veja MapeadorDeTokenWeb). */
    static final String TOKEN = "Bearer web:M123";

    private Api() {
    }

    static RequestSpecification given() {
        return RestAssured.given().header("Authorization", TOKEN);
    }

    static String unico(String prefixo) {
        return prefixo + SEQUENCIA.incrementAndGet();
    }

    static long criarCliente(String nome) {
        return post("/web/v1/clientes", Map.of("nome", nome, "cpf", unico("CPF"))).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static long criarCota(long clienteId) {
        return post("/web/v1/cotas", Map.of("numero", unico("COTA-"), "valorCredito", 250000, "clienteId", clienteId))
                .statusCode(201).extract().jsonPath().getLong("id");
    }

    static long criarProcesso(long cotaId, String tipo) {
        return post("/web/v1/processos", Map.of("cotaId", cotaId, "tipo", tipo)).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static ValidatableResponse acao(long processoId, String acao) {
        return given().when().post("/web/v1/processos/" + processoId + "/" + acao).then();
    }

    static ValidatableResponse post(String path, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(path).then();
    }
}
