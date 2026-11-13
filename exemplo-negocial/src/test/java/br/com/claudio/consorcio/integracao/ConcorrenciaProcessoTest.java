package br.com.claudio.consorcio.integracao;

import static br.com.claudio.consorcio.integracao.Api.acao;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.consorcio.domain.AlteracaoConcorrenteException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/** Duas operações no mesmo processo ao mesmo tempo: só uma vale, e a tesouraria recebe uma única ordem. */
@QuarkusTest
class ConcorrenciaProcessoTest {

    private static final Instant AGORA = Instant.now();

    @Inject
    ProcessoPagamentoRepository processos;

    @Test
    void pagamentosSimultaneosGeramUmaUnicaOrdem() throws Exception {
        long processo = processoAprovado();
        int pedidos = 6;
        Callable<Integer> pagar = () -> acao(processo, "pagar").extract().statusCode();

        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(pedidos)) {
            for (Future<Integer> resposta : executor.invokeAll(Collections.nCopies(pedidos, pagar))) {
                respostas.add(resposta.get());
            }
        }

        assertEquals(1, Collections.frequency(respostas, 200), respostas.toString());
        assertEquals(pedidos - 1, Collections.frequency(respostas, 409), respostas.toString());
        assertEquals(1, TesourariaFake.ordensDoProcesso(processo));
    }

    @Test
    void gravarSobreUmaLeituraAntigaERecusado() {
        long id = Api.criarProcesso(Api.criarCota(Api.criarCliente("Gil")), "SERVICO");
        ProcessoPagamento leituraAntiga = QuarkusTransaction.requiringNew()
                .call(() -> processos.buscarPorId(id).orElseThrow());

        acao(id, "iniciar-analise").statusCode(200);

        leituraAntiga.executar(AcaoProcesso.CANCELAR, AGORA, "desistência");
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
