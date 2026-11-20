package br.com.claudio.pagamento.domain.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.DominioException;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import br.com.claudio.pagamento.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

/** A raiz do agregado: abertura (R1), motivo (R5), histórico (R8) e que a recusa não muda nada. */
class ProcessoPagamentoTest {

    private static final Instant AGORA = Instant.parse("2026-01-02T10:00:00Z");
    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");

    @Test
    void novoComecaAbertoSemIdESemHistorico() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, " COTA-1 ", Dinheiro.de("10.00"));
        assertEquals(EstadoProcesso.ABERTO, processo.estado());
        assertEquals("COTA-1", processo.numeroCota());
        assertNull(processo.id());
        assertTrue(processo.novasTransicoes().isEmpty());
    }

    @Test
    void reconstituirGuardaOQueVeioDoBanco() {
        ProcessoPagamento processo = ProcessoPagamento.reconstituir(7L, TipoProcesso.IMOBILIARIO, "COTA-7",
                Dinheiro.de("12.30"), EstadoProcesso.AGUARDANDO_VISTORIA, 4);
        assertEquals(7L, processo.id());
        assertEquals(TipoProcesso.IMOBILIARIO, processo.tipo());
        assertEquals(Dinheiro.de("12.30"), processo.valor());
        assertEquals(EstadoProcesso.AGUARDANDO_VISTORIA, processo.estado());
        assertEquals(4, processo.versao());
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.00", "-0.01"})
    void valorPrecisaSerPositivo(String valor) {
        DominioException erro = assertThrows(DominioException.class,
                () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, "COTA-1", Dinheiro.de(valor)));
        assertEquals("Valor do processo precisa ser maior que zero", erro.getMessage());
    }

    @Test
    void cotaObrigatoriaEComTamanhoMaximo() {
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, " ", Dinheiro.de("1")));
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, "x".repeat(31), Dinheiro.de("1")));
        assertEquals(30, ProcessoPagamento.novo(TipoProcesso.SERVICO, "x".repeat(30), Dinheiro.de("1")).numeroCota().length());
    }

    @Test
    void tipoObrigatorio() {
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(null, "COTA-1", Dinheiro.de("1")));
    }

    @Test
    void executarMudaOEstadoERegistraATransicao() {
        ProcessoPagamento processo = em(EstadoProcesso.EM_ANALISE, "10.00");
        processo.executar(AcaoProcesso.REPROVAR, " ana ", AGORA, " documento ilegível ", ALCADA);

        assertEquals(EstadoProcesso.REPROVADO, processo.estado());
        assertEquals(List.of(new Transicao(EstadoProcesso.EM_ANALISE, EstadoProcesso.REPROVADO, AcaoProcesso.REPROVAR,
                AGORA, "ana", "documento ilegível")), processo.novasTransicoes());
    }

    @ParameterizedTest
    @EnumSource(value = AcaoProcesso.class, names = {"SOLICITAR_DOCUMENTO", "REPROVAR", "CANCELAR"})
    void acaoQueExigeMotivoERecusadaSemEle(AcaoProcesso acao) {
        ProcessoPagamento processo = em(EstadoProcesso.EM_ANALISE, "10.00");
        DominioException erro = assertThrows(DominioException.class,
                () -> processo.executar(acao, "ana", AGORA, "  ", ALCADA));
        assertEquals("Motivo é obrigatório para " + acao.descricao() + " um processo", erro.getMessage());
        assertNadaMudou(processo, EstadoProcesso.EM_ANALISE);
    }

    @Test
    void acaoSemMotivoObrigatorioAceitaSemEle() {
        ProcessoPagamento processo = em(EstadoProcesso.ABERTO, "10.00");
        processo.executar(AcaoProcesso.INICIAR_ANALISE, "ana", AGORA, null, ALCADA);
        assertNull(processo.novasTransicoes().getFirst().observacao());
    }

    @Test
    void transicaoInvalidaVemAntesDoMotivoENaoMudaNada() {
        ProcessoPagamento processo = em(EstadoProcesso.PAGO, "10.00");
        assertThrows(TransicaoInvalidaException.class, () -> processo.executar(AcaoProcesso.CANCELAR, "ana", AGORA, null, ALCADA));
        assertNadaMudou(processo, EstadoProcesso.PAGO);
    }

    @Test
    void autorObrigatorioNaoMudaNada() {
        ProcessoPagamento processo = em(EstadoProcesso.ABERTO, "10.00");
        assertThrows(DominioException.class, () -> processo.executar(AcaoProcesso.INICIAR_ANALISE, " ", AGORA, null, ALCADA));
        assertNadaMudou(processo, EstadoProcesso.ABERTO);
    }

    @Test
    void motivoAcimaDoTamanhoNaoMudaNada() {
        ProcessoPagamento processo = em(EstadoProcesso.EM_ANALISE, "10.00");
        assertThrows(DominioException.class,
                () -> processo.executar(AcaoProcesso.REPROVAR, "ana", AGORA, "x".repeat(501), ALCADA));
        assertNadaMudou(processo, EstadoProcesso.EM_ANALISE);
    }

    /** R4 na raiz: a alçada que o chamador entrega decide o destino, no limite exato. */
    @Test
    void alcadaDecideAoConcluirAAnalise() {
        ProcessoPagamento noLimite = em(EstadoProcesso.EM_ANALISE, "50000.00");
        noLimite.executar(AcaoProcesso.CONCLUIR_ANALISE, "ana", AGORA, null, ALCADA);
        assertEquals(EstadoProcesso.APROVADO, noLimite.estado());

        ProcessoPagamento acima = em(EstadoProcesso.EM_ANALISE, "50000.01");
        acima.executar(AcaoProcesso.CONCLUIR_ANALISE, "ana", AGORA, null, ALCADA);
        assertEquals(EstadoProcesso.AGUARDANDO_ALCADA, acima.estado());
    }

    @Test
    void historicoAcumulaNaOrdem() {
        ProcessoPagamento processo = em(EstadoProcesso.ABERTO, "10.00");
        processo.executar(AcaoProcesso.INICIAR_ANALISE, "ana", AGORA, null, ALCADA);
        processo.executar(AcaoProcesso.CONCLUIR_ANALISE, "ana", AGORA, null, ALCADA);
        processo.executar(AcaoProcesso.PAGAR, "bia", AGORA, null, ALCADA);
        assertEquals(List.of(EstadoProcesso.EM_ANALISE, EstadoProcesso.APROVADO, EstadoProcesso.PAGO),
                processo.novasTransicoes().stream().map(Transicao::para).toList());
    }

    @Test
    void acoesDisponiveisVemDoFluxoDoTipo() {
        assertEquals(Set.of(AcaoProcesso.DEVOLVER_PARA_ANALISE, AcaoProcesso.PAGAR, AcaoProcesso.CANCELAR),
                em(EstadoProcesso.APROVADO, "10.00").acoesDisponiveis());
        assertEquals(Set.of(), em(EstadoProcesso.CANCELADO, "10.00").acoesDisponiveis());
    }

    private static ProcessoPagamento em(EstadoProcesso estado, String valor) {
        return ProcessoPagamento.reconstituir(1L, TipoProcesso.SERVICO, "COTA-1", Dinheiro.de(valor), estado, 3);
    }

    private static void assertNadaMudou(ProcessoPagamento processo, EstadoProcesso estado) {
        assertEquals(estado, processo.estado());
        assertTrue(processo.novasTransicoes().isEmpty());
    }
}
