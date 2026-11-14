package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import br.com.claudio.consorcio.domain.AlteracaoConcorrenteException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.AutoresDeTeste;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import io.quarkus.arc.Arc;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Duas operações no mesmo processo ao mesmo tempo: só uma vale. Solicitar pagamento simultâneo grava uma única
 * solicitação; duas execuções simultâneas da rotina aplicam o resultado da tesouraria uma única vez.
 */
@QuarkusTest
class ConcorrenciaProcessoTest {

    private static final Instant AGORA = Instant.now();

    @Inject
    ProcessoPagamentoRepository processos;

    @Inject
    ExecutarRotinaDePagamentoUseCase rotina;

    @AfterEach
    void tesourariaNormal() {
        TesourariaFake.normalizar();
    }

    @Test
    void solicitacoesSimultaneasGravamUmaUnicaSolicitacao() throws Exception {
        long processo = processoAprovado();
        int pedidos = 6;
        Callable<Integer> solicitar = () -> Api.solicitarPagamento(processo).extract().statusCode();

        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(pedidos)) {
            for (Future<Integer> resposta : executor.invokeAll(Collections.nCopies(pedidos, solicitar))) {
                respostas.add(resposta.get());
            }
        }

        assertEquals(1, Collections.frequency(respostas, 200), respostas.toString());
        assertEquals(pedidos - 1, Collections.frequency(respostas, 409), respostas.toString());
        assertEquals(1, Api.historico(processo).stream().filter(h -> "SOLICITAR_PAGAMENTO".equals(h.get("acao"))).count());
        assertEquals(0, TesourariaFake.identificacoesRecebidas(processo).size(), "solicitar não fala com a tesouraria");

        rotina.executar();
        assertEquals("PAGO", Api.estado(processo));
        assertEquals(1, TesourariaFake.ordensAbertas(processo));
    }

    @Test
    void duasExecucoesDaRotinaAplicamOResultadoUmaUnicaVezComAMesmaIdentificacao() throws Exception {
        long processo = processoAprovado();
        Api.solicitarPagamento(processo).statusCode(200);
        // A tesouraria demora, para as duas execuções estarem com a chamada em andamento ao mesmo tempo.
        TesourariaFake.demorar(400);
        int execucoes = 2;
        CountDownLatch largada = new CountDownLatch(1);
        Callable<Void> executar = () -> {
            var contexto = Arc.container().requestContext();
            contexto.activate();
            try {
                largada.await();
                rotina.executar();
                return null;
            } finally {
                contexto.terminate();
            }
        };

        try (ExecutorService executor = Executors.newFixedThreadPool(execucoes)) {
            List<Future<Void>> futuros = new ArrayList<>();
            for (int i = 0; i < execucoes; i++) {
                futuros.add(executor.submit(executar));
            }
            largada.countDown();
            for (Future<Void> futuro : futuros) {
                futuro.get();
            }
        }

        List<Map<String, Object>> historico = Api.historico(processo);
        assertEquals("PAGO", Api.estado(processo));
        assertEquals(1, historico.stream().filter(h -> "CONFIRMAR_PAGAMENTO".equals(h.get("acao"))).count(),
                historico.toString());
        assertEquals(0, historico.stream().filter(h -> "TENTAR_PAGAMENTO".equals(h.get("acao"))).count());
        List<String> identificacoes = TesourariaFake.identificacoesRecebidas(processo);
        assertEquals(execucoes, identificacoes.size(), identificacoes.toString());
        assertEquals(1, identificacoes.stream().distinct().count(), "a mesma identificação nas duas chamadas");
        assertEquals(1, TesourariaFake.ordensAbertas(processo));
    }

    @Test
    void gravarSobreUmaLeituraAntigaERecusado() {
        long id = Api.criarProcesso(Api.criarCota(Api.criarCliente("Gil")), "SERVICO");
        ProcessoPagamento leituraAntiga = QuarkusTransaction.requiringNew()
                .call(() -> processos.buscarPorId(id).orElseThrow());

        acao(id, "iniciar-analise").statusCode(200);

        leituraAntiga.executar(AutoresDeTeste.ANA, AcaoProcesso.CANCELAR, AGORA, "desistência", Dinheiro.de("50000.00"));
        assertThrows(AlteracaoConcorrenteException.class,
                () -> QuarkusTransaction.requiringNew().run(() -> processos.salvar(leituraAntiga)));
        EstadoProcesso gravado = QuarkusTransaction.requiringNew()
                .call(() -> processos.buscarPorId(id).orElseThrow().estado());
        assertEquals(EstadoProcesso.EM_ANALISE, gravado);
    }

    private static long processoAprovado() {
        long processo = Api.criarProcesso(Api.criarCota(Api.criarCliente("Fábio")), "SERVICO");
        acao(processo, "iniciar-analise").statusCode(200);
        acao(processo, "concluir-analise").statusCode(200).body("estado", equalTo("APROVADO"));
        return processo;
    }
}
