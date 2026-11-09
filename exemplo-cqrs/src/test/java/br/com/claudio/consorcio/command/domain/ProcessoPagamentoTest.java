package br.com.claudio.consorcio.command.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.command.domain.estado.Aberto;
import br.com.claudio.consorcio.command.domain.estado.AguardandoVistoria;
import br.com.claudio.consorcio.command.domain.estado.Aprovado;
import br.com.claudio.consorcio.command.domain.estado.Cancelado;
import br.com.claudio.consorcio.command.domain.estado.EmAnalise;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;
import br.com.claudio.consorcio.command.domain.estado.Pago;
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
        assertInstanceOf(Aberto.class, processo.estado());
        assertEquals(null, processo.id());
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
            processo.iniciarAnalise();
            assertInstanceOf(EmAnalise.class, processo.estado());
            processo.concluirAnalise();
            assertInstanceOf(AguardandoVistoria.class, processo.estado());
            processo.concluirVistoria();
            assertInstanceOf(Aprovado.class, processo.estado());
            processo.pagar();
            assertInstanceOf(Pago.class, processo.estado());
            assertFalse(processo.ativo());
        }

        @Test
        void naoPodeAprovarSemVistoria() {
            processo.iniciarAnalise();
            processo.concluirAnalise();
            assertThrows(TransicaoInvalidaException.class, processo::pagar);
        }

        @Test
        void podeCancelarAguardandoVistoria() {
            processo.iniciarAnalise();
            processo.concluirAnalise();
            processo.cancelar();
            assertInstanceOf(Cancelado.class, processo.estado());
        }
    }

    @Nested
    class Servico {

        private final ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);

        @Test
        void fluxoCompletoVaiDiretoParaAprovado() {
            processo.iniciarAnalise();
            processo.concluirAnalise();
            assertInstanceOf(Aprovado.class, processo.estado());
            processo.pagar();
            assertInstanceOf(Pago.class, processo.estado());
        }

        @Test
        void naoTemEtapaDeVistoria() {
            processo.iniciarAnalise();
            assertThrows(TransicaoInvalidaException.class, processo::concluirVistoria);
            processo.concluirAnalise();
            assertThrows(TransicaoInvalidaException.class, processo::concluirVistoria);
        }
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void transicoesForaDeOrdemSaoInvalidas(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA);

        assertThrows(TransicaoInvalidaException.class, processo::concluirAnalise);
        assertThrows(TransicaoInvalidaException.class, processo::concluirVistoria);
        assertThrows(TransicaoInvalidaException.class, processo::pagar);

        processo.iniciarAnalise();
        assertThrows(TransicaoInvalidaException.class, processo::iniciarAnalise);
        assertThrows(TransicaoInvalidaException.class, processo::pagar);
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void cancelaEmEstadosNaoFinais(TipoProcesso tipo) {
        ProcessoPagamento aberto = ProcessoPagamento.novo(tipo, COTA);
        aberto.cancelar();
        assertInstanceOf(Cancelado.class, aberto.estado());

        ProcessoPagamento aprovado = aprovado(tipo);
        aprovado.cancelar();
        assertInstanceOf(Cancelado.class, aprovado.estado());
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void estadosFinaisNaoAceitamNenhumaTransicao(TipoProcesso tipo) {
        ProcessoPagamento pago = aprovado(tipo);
        pago.pagar();
        assertThrows(TransicaoInvalidaException.class, pago::cancelar);
        assertThrows(TransicaoInvalidaException.class, pago::pagar);

        ProcessoPagamento cancelado = ProcessoPagamento.novo(tipo, COTA);
        cancelado.cancelar();
        assertFalse(cancelado.ativo());
        assertThrows(TransicaoInvalidaException.class, cancelado::cancelar);
        assertThrows(TransicaoInvalidaException.class, cancelado::iniciarAnalise);
    }

    private static ProcessoPagamento aprovado(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA);
        processo.iniciarAnalise();
        processo.concluirAnalise();
        if (tipo == TipoProcesso.IMOBILIARIO) {
            processo.concluirVistoria();
        }
        return processo;
    }

    @Test
    void protocoloSoEmProcessoPago() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA);
        processo.iniciarAnalise();
        processo.concluirAnalise();
        assertThrows(DominioException.class, () -> processo.registrarProtocolo("PROT-1"));

        processo.pagar();
        assertThrows(DominioException.class, () -> processo.registrarProtocolo(" "));
        processo.registrarProtocolo("PROT-1");
        assertEquals("PROT-1", processo.protocoloPagamento());
    }

    @Test
    void reconstituirComProtocoloExigeEstadoPago() {
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, new Aprovado(), "PROT-1"));
        assertEquals("PROT-1",
                ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, new Pago(), "PROT-1").protocoloPagamento());
    }

    @Test
    void todosOsEstadosSaoReconstituiveisPeloNome() {
        for (EstadoProcesso estado : EstadoProcesso.todos()) {
            assertEquals(estado, EstadoProcesso.porNome(estado.nome()));
        }
        assertThrows(IllegalArgumentException.class, () -> EstadoProcesso.porNome("INEXISTENTE"));
    }
}
