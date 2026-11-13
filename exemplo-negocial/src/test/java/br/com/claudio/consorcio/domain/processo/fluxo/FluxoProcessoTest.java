package br.com.claudio.consorcio.domain.processo.fluxo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.PAGAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.RECEBER_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.REPROVAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.SOLICITAR_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.ABERTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.CANCELADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PENDENTE_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.REPROVADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FluxoProcessoTest {

    private final FluxoProcesso comum = FluxoProcesso.comum().construir();

    @Test
    void caminhoComum() {
        assertEquals(EM_ANALISE, comum.proximo(ABERTO, INICIAR_ANALISE));
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE));
        assertEquals(PAGO, comum.proximo(APROVADO, PAGAR));
    }

    @Test
    void acaoSemLinhaNaTabelaEInvalida() {
        TransicaoInvalidaException erro =
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(ABERTO, PAGAR));
        assertEquals("Não é possível pagar um processo no estado ABERTO", erro.getMessage());
        assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(EM_ANALISE, CONCLUIR_VISTORIA));
    }

    @ParameterizedTest
    @EnumSource(EstadoProcesso.class)
    void cancelaEmTodoEstadoNaoFinalESoNeles(EstadoProcesso estado) {
        if (estado.finalizado()) {
            assertTrue(comum.acoesDisponiveis(estado).isEmpty());
            assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(estado, CANCELAR));
        } else {
            assertEquals(CANCELADO, comum.proximo(estado, CANCELAR));
        }
    }

    @Test
    void umTipoTrocaEAcrescentaLinhasSemAfetarOComum() {
        FluxoProcesso comVistoria = FluxoProcesso.comum()
                .transicao(EM_ANALISE, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA)
                .transicao(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, APROVADO)
                .construir();

        assertEquals(AGUARDANDO_VISTORIA, comVistoria.proximo(EM_ANALISE, CONCLUIR_ANALISE));
        assertEquals(APROVADO, comVistoria.proximo(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA));
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE));
    }

    @Test
    void documentoEReprovacaoNaAnalise() {
        assertEquals(PENDENTE_DOCUMENTO, comum.proximo(EM_ANALISE, SOLICITAR_DOCUMENTO));
        assertEquals(EM_ANALISE, comum.proximo(PENDENTE_DOCUMENTO, RECEBER_DOCUMENTO));
        assertEquals(REPROVADO, comum.proximo(EM_ANALISE, REPROVAR));
        assertEquals(Set.of(SOLICITAR_DOCUMENTO, REPROVAR, CONCLUIR_ANALISE, CANCELAR),
                comum.acoesDisponiveis(EM_ANALISE));
        assertEquals(Set.of(RECEBER_DOCUMENTO, CANCELAR), comum.acoesDisponiveis(PENDENTE_DOCUMENTO));
    }

    @Test
    void reprovadoEFinalESoReprovaDaAnalise() {
        assertTrue(REPROVADO.finalizado());
        assertTrue(comum.acoesDisponiveis(REPROVADO).isEmpty());
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            if (estado != EM_ANALISE) {
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(estado, REPROVAR), estado.name());
            }
        }
    }

    @Test
    void acoesDisponiveisSaemDaTabela() {
        assertEquals(Set.of(INICIAR_ANALISE, CANCELAR), comum.acoesDisponiveis(ABERTO));
        assertEquals(Set.of(DEVOLVER_PARA_ANALISE, PAGAR, CANCELAR), comum.acoesDisponiveis(APROVADO));
    }

    @Test
    void estadoFinalNaoTemSaida() {
        assertThrows(IllegalArgumentException.class,
                () -> FluxoProcesso.comum().transicao(PAGO, INICIAR_ANALISE, EM_ANALISE));
    }
}
