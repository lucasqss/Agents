package br.com.claudio.consorcio.application.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ResultadoPagamento;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** O resultado da tesouraria só é gravado quando é da solicitação que o processo aguarda. */
class AplicarResultadoPagamentoUseCaseTest {

    private final ProcessosEmMemoria repositorio = new ProcessosEmMemoria();
    private final AplicarResultadoPagamentoUseCase aplicar = new AplicarResultadoPagamentoUseCase(repositorio);

    @Test
    void resultadoDaSolicitacaoAguardadaEhAplicadoEGravado() {
        ProcessoPagamento processo = repositorio.solicitado(1);
        IdentificacaoSolicitacao aguardada = processo.identificacaoSolicitacao().orElseThrow();

        assertTrue(aplicar.executar(1L, aguardada, new ResultadoPagamento.Confirmado("778899")));

        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(1L).estado());
        assertEquals(1, repositorio.salvamentos);
    }

    @Test
    void resultadoDeOutraSolicitacaoEhIgnoradoSemGravar() {
        repositorio.solicitado(1);
        IdentificacaoSolicitacao antiga = new IdentificacaoSolicitacao(UUID.randomUUID());

        assertFalse(aplicar.executar(1L, antiga, new ResultadoPagamento.Confirmado("778899")));

        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, repositorio.gravados.get(1L).estado());
        assertEquals(0, repositorio.salvamentos);
    }
}
