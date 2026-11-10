package br.com.claudio.consorcio.domain.processo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.PAGAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.ABERTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.CANCELADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProcessoPagamentoTest {

    private static final Long COTA = 1L;

    @Test
    void novoProcessoComecaAbertoESemId() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);

        assertEquals(TipoProcesso.SERVICO, processo.tipo());
        assertEquals(ABERTO, processo.estado());
        assertNull(processo.id());
        assertTrue(processo.ativo());
    }

    @Test
    void exigeTipoECota() {
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(null, COTA));
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, null));
    }

    @Nested
    class Imobiliario {

        private final ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.IMOBILIARIO, COTA);

        @Test
        void fluxoCompletoPassaPelaVistoria() {
            processo.executar(INICIAR_ANALISE);
            assertEquals(EM_ANALISE, processo.estado());

            processo.executar(CONCLUIR_ANALISE);
            assertEquals(AGUARDANDO_VISTORIA, processo.estado());

            processo.executar(CONCLUIR_VISTORIA);
            assertEquals(APROVADO, processo.estado());

            processo.pagar("PROT-1");
            assertEquals(PAGO, processo.estado());
            assertFalse(processo.ativo());
        }

        @Test
        void naoPodePagarSemVistoria() {
            processo.executar(INICIAR_ANALISE);
            processo.executar(CONCLUIR_ANALISE);

            invalida(processo, PAGAR);
        }

        @Test
        void podeCancelarAguardandoVistoria() {
            processo.executar(INICIAR_ANALISE);
            processo.executar(CONCLUIR_ANALISE);
            processo.executar(CANCELAR);

            assertEquals(CANCELADO, processo.estado());
        }
    }

    @Nested
    class Servico {

        private final ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);

        @Test
        void fluxoCompletoVaiDiretoParaAprovado() {
            processo.executar(INICIAR_ANALISE);
            processo.executar(CONCLUIR_ANALISE);
            assertEquals(APROVADO, processo.estado());

            processo.pagar("PROT-1");
            assertEquals(PAGO, processo.estado());
        }

        @Test
        void naoTemEtapaDeVistoria() {
            processo.executar(INICIAR_ANALISE);
            invalida(processo, CONCLUIR_VISTORIA);

            processo.executar(CONCLUIR_ANALISE);
            invalida(processo, CONCLUIR_VISTORIA);
        }
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void transicoesForaDeOrdemSaoInvalidas(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA);

        invalida(processo, CONCLUIR_ANALISE);
        invalida(processo, CONCLUIR_VISTORIA);
        invalida(processo, PAGAR);

        processo.executar(INICIAR_ANALISE);
        invalida(processo, INICIAR_ANALISE);
        invalida(processo, PAGAR);
        assertEquals(EM_ANALISE, processo.estado(), "ação recusada não muda o estado");
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void cancelaEmEstadosNaoFinais(TipoProcesso tipo) {
        ProcessoPagamento aberto = ProcessoPagamento.novo(tipo, COTA);
        aberto.executar(CANCELAR);
        assertEquals(CANCELADO, aberto.estado());

        ProcessoPagamento aprovado = aprovado(tipo);
        aprovado.executar(CANCELAR);
        assertEquals(CANCELADO, aprovado.estado());
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void estadosFinaisNaoAceitamNenhumaAcao(TipoProcesso tipo) {
        ProcessoPagamento pago = aprovado(tipo);
        pago.pagar("PROT-1");
        assertTrue(pago.acoesDisponiveis().isEmpty());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            invalida(pago, acao);
        }

        ProcessoPagamento cancelado = ProcessoPagamento.novo(tipo, COTA);
        cancelado.executar(CANCELAR);
        assertFalse(cancelado.ativo());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            invalida(cancelado, acao);
        }
    }

    @Test
    void acoesDisponiveisDependemDoTipoEDoEstado() {
        ProcessoPagamento servico = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);
        assertEquals(Set.of(INICIAR_ANALISE, CANCELAR), servico.acoesDisponiveis());

        ProcessoPagamento imobiliario = ProcessoPagamento.novo(TipoProcesso.IMOBILIARIO, COTA);
        imobiliario.executar(INICIAR_ANALISE);
        imobiliario.executar(CONCLUIR_ANALISE);
        assertEquals(Set.of(CONCLUIR_VISTORIA, CANCELAR), imobiliario.acoesDisponiveis());
    }

    @Test
    void pagarMudaOEstadoEGuardaOProtocoloJuntos() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);
        assertThrows(DominioException.class, () -> processo.pagar(" "));
        assertThrows(DominioException.class, () -> processo.pagar(null));
        assertEquals(APROVADO, processo.estado());

        processo.pagar("PROT-1");
        assertEquals(PAGO, processo.estado());
        assertEquals("PROT-1", processo.protocoloPagamento());
    }

    @Test
    void naoFicaPagoSemProtocolo() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);

        assertThrows(DominioException.class, () -> processo.executar(PAGAR));
        assertEquals(APROVADO, processo.estado());
        assertNull(processo.protocoloPagamento());
    }

    @Test
    void exigirQuePodeConfereSemAplicar() {
        ProcessoPagamento aberto = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);
        assertThrows(TransicaoInvalidaException.class, () -> aberto.exigirQuePode(PAGAR));

        ProcessoPagamento aprovado = aprovado(TipoProcesso.SERVICO);
        aprovado.exigirQuePode(PAGAR);
        assertEquals(APROVADO, aprovado.estado());
    }

    @Test
    void reconstituirExigeProtocoloSeESomenteSePago() {
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, APROVADO, "PROT-1", 0));
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, PAGO, null, 0));

        ProcessoPagamento pago = ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, PAGO, "PROT-1", 3);
        assertEquals("PROT-1", pago.protocoloPagamento());
        assertEquals(3, pago.versao());
    }

    private static void invalida(ProcessoPagamento processo, AcaoProcesso acao) {
        assertThrows(TransicaoInvalidaException.class, () -> processo.executar(acao));
    }

    private static ProcessoPagamento aprovado(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA);
        processo.executar(INICIAR_ANALISE);
        processo.executar(CONCLUIR_ANALISE);
        if (processo.acoesDisponiveis().contains(CONCLUIR_VISTORIA)) {
            processo.executar(CONCLUIR_VISTORIA);
        }
        return processo;
    }
}
