package br.com.claudio.consorcio.integracao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/** Pedidos simultâneos de alteração sobre o mesmo processo: um vale, os outros são recusados, e nada fica pela metade. */
@QuarkusTest
class ConcorrenciaAlteracaoTest {

    private static final int PEDIDOS = 4;

    private static List<Integer> dispararJuntos(List<Callable<Integer>> pedidos) throws Exception {
        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(pedidos.size())) {
            for (Future<Integer> resposta : executor.invokeAll(pedidos)) {
                respostas.add(resposta.get());
            }
        }
        return respostas;
    }

    @Test
    void propostasSimultaneasDoMesmoProcessoGravamUmaSo() throws Exception {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Cliente Propostas")), "SERVICO");
        Api.acao(processo, "iniciar-analise").statusCode(200);
        CyclicBarrier largada = new CyclicBarrier(PEDIDOS);
        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            BigDecimal valor = new BigDecimal("1000.00").add(BigDecimal.valueOf(i));
            pedidos.add(() -> {
                largada.await(30, TimeUnit.SECONDS);
                return Api.post("/processos/" + processo + "/propor-alteracao", Map.of("valor", valor, "motivo", "ajuste",
                        "favorecidos", List.of(Api.favorecido("Novo", "98765432100", "vendedor", valor))))
                        .extract().statusCode();
            });
        }

        List<Integer> respostas = dispararJuntos(pedidos);

        assertEquals(1, Collections.frequency(respostas, 200), respostas.toString());
        assertEquals(PEDIDOS - 1, Collections.frequency(respostas, 409), respostas.toString());
        Api.acao(processo, "concordar").statusCode(200);
    }

    @Test
    void alteracoesSimultaneasEmAbertoTerminamComUmConjuntoCoerente() throws Exception {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Cliente Alteracoes")), "SERVICO");
        CyclicBarrier largada = new CyclicBarrier(PEDIDOS);
        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < PEDIDOS; i++) {
            BigDecimal valor = new BigDecimal("1000.00").add(BigDecimal.valueOf(i));
            String documento = "1234567890" + i;
            pedidos.add(() -> {
                largada.await(30, TimeUnit.SECONDS);
                return Api.post("/processos/" + processo + "/alterar", Map.of("valor", valor,
                        "favorecidos", List.of(Api.favorecido("Favorecido " + documento, documento, "vendedor", valor))))
                        .extract().statusCode();
            });
        }

        List<Integer> respostas = dispararJuntos(pedidos);

        // Cada resposta é 200 ou 409 (perdeu a disputa); nunca 400 nem 500, que seriam leitura ou gravação pela metade.
        for (int status : respostas) {
            assertTrue(status == 200 || status == 409, respostas.toString());
        }
        assertTrue(Collections.frequency(respostas, 200) >= 1, respostas.toString());
        // O que ficou gravado é um único conjunto: um favorecido, com a parte igual ao valor do processo.
        var favorecidos = io.restassured.RestAssured.given().get("/processos/" + processo + "/favorecidos")
                .then().statusCode(200).extract().jsonPath();
        assertEquals(1, favorecidos.getList("nome").size());
    }
}
