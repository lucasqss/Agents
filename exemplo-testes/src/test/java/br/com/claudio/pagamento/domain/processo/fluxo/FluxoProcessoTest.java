package br.com.claudio.pagamento.domain.processo.fluxo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.pagamento.domain.Dinheiro;
import org.junit.jupiter.api.Test;

/** O Builder e a tabela: as garantias que valem para qualquer tipo, presente ou futuro. */
class FluxoProcessoTest {

    private static final ContextoDaTransicao SEM_ALCADA = new ContextoDaTransicao(Dinheiro.de("10.00"), null);

    @Test
    void estadoFinalNaoAceitaLinha() {
        FluxoProcesso.Builder builder = FluxoProcesso.comum();
        assertThrows(IllegalArgumentException.class,
                () -> builder.transicao(EstadoProcesso.PAGO, AcaoProcesso.CANCELAR, EstadoProcesso.ABERTO));
    }

    @Test
    void linhaNovaTrocaADoMesmoEstadoEAcao() {
        FluxoProcesso fluxo = FluxoProcesso.comum()
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, EstadoProcesso.PENDENTE_DOCUMENTO)
                .construir();
        assertEquals(EstadoProcesso.PENDENTE_DOCUMENTO,
                fluxo.proximo(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, SEM_ALCADA));
    }

    @Test
    void acaoForaDaTabelaDizAAcaoEOEstado() {
        FluxoProcesso fluxo = FluxoProcesso.comum().construir();
        TransicaoInvalidaException erro = assertThrows(TransicaoInvalidaException.class,
                () -> fluxo.proximo(EstadoProcesso.ABERTO, AcaoProcesso.PAGAR, SEM_ALCADA));
        assertEquals("Não é possível pagar um processo no estado ABERTO", erro.getMessage());
    }

    @Test
    void linhaCondicionalSemAlcadaEhErroDeProgramacao() {
        FluxoProcesso fluxo = FluxoProcesso.comum().construir();
        assertThrows(IllegalStateException.class,
                () -> fluxo.proximo(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE, SEM_ALCADA));
    }

    @Test
    void tabelaFechadaNaoMuda() {
        FluxoProcesso fluxo = FluxoProcesso.comum().construir();
        assertThrows(UnsupportedOperationException.class,
                () -> fluxo.acoesDisponiveis(EstadoProcesso.ABERTO).add(AcaoProcesso.PAGAR));
    }
}
