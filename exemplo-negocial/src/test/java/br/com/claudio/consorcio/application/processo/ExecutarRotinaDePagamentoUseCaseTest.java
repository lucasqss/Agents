package br.com.claudio.consorcio.application.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.gateway.tesouraria.OrdemPagamentoResponse;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** A rotina isola a falha de um processo: os demais seguem, e o que falhou continua esperando a próxima rodada. */
class ExecutarRotinaDePagamentoUseCaseTest {

    private final ProcessosEmMemoria repositorio = new ProcessosEmMemoria();
    private final List<String> pedidos = new ArrayList<>();

    private ExecutarRotinaDePagamentoUseCase rotinaComTesouraria(OrdemPagamentoResponse resposta) {
        TesourariaGateway gateway = new TesourariaGateway(request -> {
            pedidos.add(request.requestId() + "|" + request.processId());
            return resposta;
        });
        return new ExecutarRotinaDePagamentoUseCase(repositorio, gateway, new AplicarResultadoPagamentoUseCase(repositorio));
    }

    @Test
    void falhaEmUmProcessoNaoImpedeOsDemais() {
        repositorio.solicitado(1);
        repositorio.solicitado(2);
        repositorio.solicitado(3);
        repositorio.falhaAoLer.add(2L);

        rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null)).executar();

        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(1L).estado());
        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, repositorio.gravados.get(2L).estado());
        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(3L).estado());
        assertEquals(2, pedidos.size());
    }

    @Test
    void oProcessoQueFalhouEhTentadoDeNovoNaProximaRodada() {
        repositorio.solicitado(1);
        repositorio.falhaAoLer.add(1L);
        ExecutarRotinaDePagamentoUseCase rotina = rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null));
        rotina.executar();
        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, repositorio.gravados.get(1L).estado());

        repositorio.falhaAoLer.clear();
        rotina.executar();

        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(1L).estado());
    }

    @Test
    void cadaRodadaRepeteAMesmaIdentificacaoEORespostaIncoerenteViraTentativa() {
        ProcessoPagamento processo = repositorio.solicitado(7);
        ExecutarRotinaDePagamentoUseCase rotina = rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", null, null));

        rotina.executar();
        rotina.executar();

        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(2, pedidos.size());
        assertEquals(pedidos.get(0), pedidos.get(1), "mesma identificação, mesmo processo");
        assertTrue(pedidos.get(0).startsWith(processo.identificacaoSolicitacao().orElseThrow().texto()));
        assertEquals(2, processo.novasTransicoes().stream().filter(t -> t.acao() == AcaoProcesso.TENTAR_PAGAMENTO).count());
    }

    @Test
    void semProcessoEmPagamentoSolicitadoNaoChamaATesouraria() {
        rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null)).executar();

        assertTrue(pedidos.isEmpty());
    }
}
