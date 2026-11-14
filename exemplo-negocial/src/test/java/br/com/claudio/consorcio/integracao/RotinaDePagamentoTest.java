package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.historico;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import br.com.claudio.consorcio.integracao.TesourariaFake.Modo;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Pagamento como saga (ADR 002): solicitar só grava; a rotina fala com a tesouraria. O agendamento está desligado nos
 * testes de integração ({@code %test.pagamento.rotina.intervalo=off}): cada teste chama o caso de uso da rotina quando
 * quer uma rodada, sem depender de tempo.
 */
@QuarkusTest
class RotinaDePagamentoTest {

    @Inject
    ExecutarRotinaDePagamentoUseCase rotina;

    /** Deixa a tesouraria normal e conclui o que ficou esperando, para um teste não atrapalhar o seguinte. */
    @AfterEach
    void concluirOQueFicouEsperando() {
        TesourariaFake.normalizar();
        rotina.executar();
    }

    private static long processoAprovado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Rotina")), "SERVICO");
        Api.acao(processo, "iniciar-analise").statusCode(200);
        Api.acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("APROVADO"));
        return processo;
    }

    private static long processoSolicitado() {
        long processo = processoAprovado();
        Api.solicitarPagamento(processo).statusCode(200).body("estado", equalTo("PAGAMENTO_SOLICITADO"));
        return processo;
    }

    /** Ações do histórico desde a solicitação do pagamento (antes dela está só o caminho até aprovado). */
    private static List<String> acoesDoHistorico(long processo) {
        List<String> acoes = historico(processo).stream().map(h -> (String) h.get("acao")).toList();
        return acoes.subList(acoes.indexOf("SOLICITAR_PAGAMENTO"), acoes.size());
    }

    @Test
    void solicitarSoGravaENaoDependeDaTesouraria() {
        TesourariaFake.responder(Modo.INDISPONIVEL);
        long processo = processoAprovado();

        Api.solicitarPagamento(processo).statusCode(200).body("estado", equalTo("PAGAMENTO_SOLICITADO"));

        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        assertTrue(TesourariaFake.identificacoesRecebidas(processo).isEmpty(), "solicitar não envia nada à tesouraria");
        given().when().get("/processos/" + processo + "/acoes").then().statusCode(200).body("acoes.size()", equalTo(0));
    }

    @Test
    void oEndpointAntigoDePagarNaoExisteMais() {
        long processo = processoAprovado();

        Api.acao(processo, "pagar").statusCode(404);

        assertEquals("APROVADO", Api.estado(processo));
    }

    @Test
    void aRotinaConfirmaEPagaComOProtocoloDaTesouraria() {
        long processo = processoSolicitado();

        rotina.executar();

        assertEquals("PAGO", Api.estado(processo));
        assertEquals(List.of("SOLICITAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"), acoesDoHistorico(processo));
        given().queryParam("estado", "PAGO").queryParam("tamanho", 100).when().get("/processos").then().statusCode(200)
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), startsWith("TES-"));
        assertEquals(1, TesourariaFake.identificacoesRecebidas(processo).size());
    }

    @Test
    void aRotinaSoOlhaProcessosEmPagamentoSolicitado() {
        long aprovado = processoAprovado();
        long solicitado = processoSolicitado();

        rotina.executar();

        assertTrue(TesourariaFake.identificacoesRecebidas(aprovado).isEmpty());
        assertEquals(1, TesourariaFake.identificacoesRecebidas(solicitado).size());
        assertEquals("APROVADO", Api.estado(aprovado));
    }

    @Test
    void recusaVoltaAAprovadoComOMotivoENovaSolicitacaoTemIdentificacaoNova() {
        TesourariaFake.responder(Modo.RECUSAR);
        long processo = processoSolicitado();

        rotina.executar();

        assertEquals("APROVADO", Api.estado(processo));
        Map<String, Object> recusa = historico(processo).getLast();
        assertEquals("RECUSAR_PAGAMENTO", recusa.get("acao"));
        assertEquals(TesourariaFake.MOTIVO_DA_RECUSA, recusa.get("observacao"));

        Api.solicitarPagamento(processo).statusCode(200);
        TesourariaFake.responder(Modo.CONFIRMAR);
        rotina.executar();

        assertEquals("PAGO", Api.estado(processo));
        List<String> identificacoes = TesourariaFake.identificacoesRecebidas(processo);
        assertEquals(2, identificacoes.size());
        assertNotEquals(identificacoes.get(0), identificacoes.get(1), "cada solicitação tem identificação nova");
    }

    @Test
    void tesourariaIndisponivelNaoRecusaNemPaga_eARotinaRepeteAMesmaIdentificacao() {
        TesourariaFake.responder(Modo.INDISPONIVEL);
        long processo = processoSolicitado();

        rotina.executar();
        rotina.executar();
        rotina.executar();

        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        List<Map<String, Object>> historico = historico(processo);
        assertEquals(3, historico.stream().filter(h -> "TENTAR_PAGAMENTO".equals(h.get("acao"))).count());
        historico.stream().filter(h -> "TENTAR_PAGAMENTO".equals(h.get("acao"))).forEach(tentativa -> {
            assertEquals("PAGAMENTO_SOLICITADO", tentativa.get("de"));
            assertEquals("PAGAMENTO_SOLICITADO", tentativa.get("para"));
            assertNotNull(tentativa.get("observacao"));
            assertFalse(((String) tentativa.get("observacao")).isBlank());
        });
        given().queryParam("estado", "PAGAMENTO_SOLICITADO").queryParam("tamanho", 100).when().get("/processos").then()
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), nullValue());

        TesourariaFake.responder(Modo.CONFIRMAR);
        rotina.executar();

        assertEquals("PAGO", Api.estado(processo));
        List<String> identificacoes = TesourariaFake.identificacoesRecebidas(processo);
        assertEquals(4, identificacoes.size());
        assertEquals(1, identificacoes.stream().distinct().count(), "a mesma identificação em todas as tentativas");
        assertEquals(1, TesourariaFake.ordensAbertas(processo));
    }

    @Test
    void quedaEntreAChamadaEAGravacao_aProximaRodadaRecebeAlreadyProcessedEPaga() {
        // A tesouraria abriu a ordem, mas a resposta não chegou: o processo não foi pago aqui.
        TesourariaFake.responder(Modo.ABRIR_ORDEM_E_CAIR);
        long processo = processoSolicitado();
        rotina.executar();
        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        assertEquals(1, TesourariaFake.ordensAbertas(processo));

        TesourariaFake.responder(Modo.CONFIRMAR);
        rotina.executar();

        assertEquals("PAGO", Api.estado(processo));
        assertEquals(1, TesourariaFake.ordensAbertas(processo), "a mesma identificação não abre uma segunda ordem");
        assertEquals(1, TesourariaFake.identificacoesRecebidas(processo).stream().distinct().count());
        assertEquals(List.of("SOLICITAR_PAGAMENTO", "TENTAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"), acoesDoHistorico(processo));
    }

    @Test
    void respostaIncoerenteNaoEAplicadaEFicaNoHistorico() {
        TesourariaFake.responder(Modo.CONFIRMAR_SEM_PROTOCOLO);
        long processo = processoSolicitado();
        rotina.executar();

        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        TesourariaFake.responder(Modo.RECUSAR_SEM_MOTIVO);
        rotina.executar();

        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        assertEquals(List.of("SOLICITAR_PAGAMENTO", "TENTAR_PAGAMENTO", "TENTAR_PAGAMENTO"), acoesDoHistorico(processo));
        given().queryParam("estado", "PAGAMENTO_SOLICITADO").queryParam("tamanho", 100).when().get("/processos").then()
                .body("itens.find { it.id == %s }.protocoloPagamento".formatted(processo), nullValue());

        TesourariaFake.responder(Modo.CONFIRMAR);
        rotina.executar();
        assertEquals("PAGO", Api.estado(processo));
    }

    @Test
    void oFiltroEOsRotulosConhecemPagamentoSolicitado() {
        TesourariaFake.responder(Modo.INDISPONIVEL);
        long processo = processoSolicitado();

        given().queryParam("estado", "PAGAMENTO_SOLICITADO").queryParam("tamanho", 100).when().get("/processos").then()
                .statusCode(200)
                .body("itens.id", hasItem((int) processo))
                .body("itens.estado", not(hasItems("APROVADO", "PAGO")));
    }

    @Test
    void circuitoAbertoViraTentativaSemChamarATesouraria() throws InterruptedException {
        TesourariaFake.responder(Modo.INDISPONIVEL);
        long processo = processoSolicitado();
        // O circuito abre depois de 10 chamadas com metade ou mais de falhas.
        for (int i = 0; i < 12; i++) {
            rotina.executar();
        }
        int chamadasAntes = TesourariaFake.identificacoesRecebidas(processo).size();
        assertTrue(chamadasAntes <= 10, "o circuito abriu: " + chamadasAntes);

        rotina.executar();

        assertEquals(chamadasAntes, TesourariaFake.identificacoesRecebidas(processo).size(),
                "com o circuito aberto a rotina não chama a tesouraria");
        assertEquals("PAGAMENTO_SOLICITADO", Api.estado(processo));
        assertEquals(13, historico(processo).stream().filter(h -> "TENTAR_PAGAMENTO".equals(h.get("acao"))).count());

        // Espera o circuito fechar de novo (delay do %test), para não atrapalhar os outros testes.
        TesourariaFake.normalizar();
        for (int i = 0; i < 40 && !"PAGO".equals(Api.estado(processo)); i++) {
            Thread.sleep(250);
            rotina.executar();
        }
        assertEquals("PAGO", Api.estado(processo));
    }
}
