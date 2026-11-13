package br.com.claudio.consorcio.domain.processo;

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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class ProcessoPagamentoTest {

    private static final Long COTA = 1L;
    private static final Dinheiro VALOR = Dinheiro.de("30000.00");
    private static final Instant AGORA = Instant.parse("2026-01-15T12:00:00Z");

    @Test
    void novoProcessoComecaAbertoESemId() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);

        assertEquals(TipoProcesso.SERVICO, processo.tipo());
        assertEquals(ABERTO, processo.estado());
        assertNull(processo.id());
        assertTrue(processo.ativo());
    }

    @Test
    void exigeTipoECota() {
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(null, COTA, VALOR));
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, null, VALOR));
    }

    @Test
    void guardaOValorInformado() {
        assertEquals(Dinheiro.de("12500.50"),
                ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, Dinheiro.de("12500.50")).valor());
    }

    @Test
    void valorEhObrigatorioEMaiorQueZero() {
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, null));
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, Dinheiro.ZERO));
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, Dinheiro.de("-0.01")));
    }

    @Test
    void valorTemNoMaximoDuasCasas() {
        assertThrows(DominioException.class, () -> Dinheiro.de("100.005"));
        assertEquals(Dinheiro.de("0.01"), ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, Dinheiro.de("0.01")).valor());
    }

    @Nested
    class Imobiliario {

        private final ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.IMOBILIARIO, COTA, VALOR);

        @Test
        void fluxoCompletoPassaPelaVistoria() {
            processo.executar(INICIAR_ANALISE, AGORA, null);
            assertEquals(EM_ANALISE, processo.estado());

            processo.executar(CONCLUIR_ANALISE, AGORA, null);
            assertEquals(AGUARDANDO_VISTORIA, processo.estado());

            processo.executar(CONCLUIR_VISTORIA, AGORA, null);
            assertEquals(APROVADO, processo.estado());

            processo.pagar("PROT-1", AGORA);
            assertEquals(PAGO, processo.estado());
            assertFalse(processo.ativo());
        }

        @Test
        void naoPodePagarSemVistoria() {
            processo.executar(INICIAR_ANALISE, AGORA, null);
            processo.executar(CONCLUIR_ANALISE, AGORA, null);

            invalida(processo, PAGAR);
        }

        @Test
        void podeCancelarAguardandoVistoria() {
            processo.executar(INICIAR_ANALISE, AGORA, null);
            processo.executar(CONCLUIR_ANALISE, AGORA, null);
            processo.executar(CANCELAR, AGORA, "desistência");

            assertEquals(CANCELADO, processo.estado());
        }
    }

    @Nested
    class Servico {

        private final ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);

        @Test
        void fluxoCompletoVaiDiretoParaAprovado() {
            processo.executar(INICIAR_ANALISE, AGORA, null);
            processo.executar(CONCLUIR_ANALISE, AGORA, null);
            assertEquals(APROVADO, processo.estado());

            processo.pagar("PROT-1", AGORA);
            assertEquals(PAGO, processo.estado());
        }

        @Test
        void naoTemEtapaDeVistoria() {
            processo.executar(INICIAR_ANALISE, AGORA, null);
            invalida(processo, CONCLUIR_VISTORIA);

            processo.executar(CONCLUIR_ANALISE, AGORA, null);
            invalida(processo, CONCLUIR_VISTORIA);
        }
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void transicoesForaDeOrdemSaoInvalidas(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA, VALOR);

        invalida(processo, CONCLUIR_ANALISE);
        invalida(processo, CONCLUIR_VISTORIA);
        invalida(processo, PAGAR);

        processo.executar(INICIAR_ANALISE, AGORA, null);
        invalida(processo, INICIAR_ANALISE);
        invalida(processo, PAGAR);
        assertEquals(EM_ANALISE, processo.estado(), "ação recusada não muda o estado");
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void cancelaEmEstadosNaoFinais(TipoProcesso tipo) {
        ProcessoPagamento aberto = ProcessoPagamento.novo(tipo, COTA, VALOR);
        aberto.executar(CANCELAR, AGORA, "desistência");
        assertEquals(CANCELADO, aberto.estado());

        ProcessoPagamento aprovado = aprovado(tipo);
        aprovado.executar(CANCELAR, AGORA, "desistência");
        assertEquals(CANCELADO, aprovado.estado());
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void estadosFinaisNaoAceitamNenhumaAcao(TipoProcesso tipo) {
        ProcessoPagamento pago = aprovado(tipo);
        pago.pagar("PROT-1", AGORA);
        assertTrue(pago.acoesDisponiveis().isEmpty());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            invalida(pago, acao);
        }

        ProcessoPagamento cancelado = ProcessoPagamento.novo(tipo, COTA, VALOR);
        cancelado.executar(CANCELAR, AGORA, "desistência");
        assertFalse(cancelado.ativo());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            invalida(cancelado, acao);
        }
    }

    @Test
    void acoesDisponiveisDependemDoTipoEDoEstado() {
        ProcessoPagamento servico = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);
        assertEquals(Set.of(INICIAR_ANALISE, CANCELAR), servico.acoesDisponiveis());

        ProcessoPagamento imobiliario = ProcessoPagamento.novo(TipoProcesso.IMOBILIARIO, COTA, VALOR);
        imobiliario.executar(INICIAR_ANALISE, AGORA, null);
        imobiliario.executar(CONCLUIR_ANALISE, AGORA, null);
        assertEquals(Set.of(CONCLUIR_VISTORIA, DEVOLVER_PARA_ANALISE, CANCELAR), imobiliario.acoesDisponiveis());
    }

    @Test
    void pagarMudaOEstadoEGuardaOProtocoloJuntos() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);
        assertThrows(DominioException.class, () -> processo.pagar(" ", AGORA));
        assertThrows(DominioException.class, () -> processo.pagar(null, AGORA));
        assertEquals(APROVADO, processo.estado());

        processo.pagar("PROT-1", AGORA);
        assertEquals(PAGO, processo.estado());
        assertEquals("PROT-1", processo.protocoloPagamento());
    }

    @Test
    void naoFicaPagoSemProtocolo() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);

        assertThrows(DominioException.class, () -> processo.executar(PAGAR, AGORA, null));
        assertEquals(APROVADO, processo.estado());
        assertNull(processo.protocoloPagamento());
    }

    @Test
    void exigirQuePodeConfereSemAplicar() {
        ProcessoPagamento aberto = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);
        assertThrows(TransicaoInvalidaException.class, () -> aberto.exigirQuePode(PAGAR));

        ProcessoPagamento aprovado = aprovado(TipoProcesso.SERVICO);
        aprovado.exigirQuePode(PAGAR);
        assertEquals(APROVADO, aprovado.estado());
    }

    @Test
    void reconstituirExigeProtocoloSeESomenteSePago() {
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, VALOR, APROVADO, "PROT-1", 0));
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, VALOR, PAGO, null, 0));

        ProcessoPagamento pago = ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, VALOR, PAGO, "PROT-1", 3);
        assertEquals("PROT-1", pago.protocoloPagamento());
        assertEquals(3, pago.versao());
    }

    @Test
    void aprovadoPodeSerDevolvidoParaAnaliseEmQualquerTipo() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            ProcessoPagamento processo = aprovado(tipo);
            processo.executar(DEVOLVER_PARA_ANALISE, AGORA, "Falta documento");
            assertEquals(EM_ANALISE, processo.estado(), tipo.name());
        }
    }

    @Test
    void soOImobiliarioDevolveDaVistoria() {
        ProcessoPagamento imobiliario = ProcessoPagamento.novo(TipoProcesso.IMOBILIARIO, COTA, VALOR);
        imobiliario.executar(INICIAR_ANALISE, AGORA, null);
        imobiliario.executar(CONCLUIR_ANALISE, AGORA, null);
        imobiliario.executar(DEVOLVER_PARA_ANALISE, AGORA, "documento divergente");
        assertEquals(EM_ANALISE, imobiliario.estado());

        ProcessoPagamento servico = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);
        servico.executar(INICIAR_ANALISE, AGORA, null);
        invalida(servico, DEVOLVER_PARA_ANALISE);
    }

    @Test
    void cadaTransicaoFicaRegistradaParaOHistorico() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);
        assertTrue(processo.novasTransicoes().isEmpty());

        processo.executar(INICIAR_ANALISE, AGORA, "  Documentos recebidos  ");
        invalida(processo, PAGAR);
        processo.executar(CONCLUIR_ANALISE, AGORA, " ");
        processo.pagar("PROT-1", AGORA.plusSeconds(60));

        assertEquals(List.of(
                new Transicao(ABERTO, EM_ANALISE, INICIAR_ANALISE, AGORA, "Documentos recebidos"),
                new Transicao(EM_ANALISE, APROVADO, CONCLUIR_ANALISE, AGORA, null),
                new Transicao(APROVADO, PAGO, PAGAR, AGORA.plusSeconds(60), null)),
                processo.novasTransicoes());
    }

    @Test
    void transicaoExigeDataELimitaAObservacao() {
        ProcessoPagamento processo = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);

        assertThrows(DominioException.class, () -> processo.executar(INICIAR_ANALISE, null, null));
        assertThrows(DominioException.class, () -> processo.executar(INICIAR_ANALISE, AGORA, "x".repeat(501)));
        assertEquals(ABERTO, processo.estado());
        assertTrue(processo.novasTransicoes().isEmpty());
    }

    @Test
    void processoReconstituidoNaoTrazTransicoesAntigas() {
        ProcessoPagamento lido = ProcessoPagamento.reconstituir(1L, COTA, TipoProcesso.SERVICO, VALOR, EM_ANALISE, null, 1);
        assertTrue(lido.novasTransicoes().isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void solicitarDocumentoLevaAPendenteEReceberVoltaParaAnalise(TipoProcesso tipo) {
        ProcessoPagamento processo = emAnalise(tipo);

        processo.executar(SOLICITAR_DOCUMENTO, AGORA, "certidão vencida");
        assertEquals(PENDENTE_DOCUMENTO, processo.estado());
        assertEquals(Set.of(RECEBER_DOCUMENTO, CANCELAR), processo.acoesDisponiveis());

        processo.executar(RECEBER_DOCUMENTO, AGORA, null);
        assertEquals(EM_ANALISE, processo.estado());
        assertEquals(List.of(
                new Transicao(EM_ANALISE, PENDENTE_DOCUMENTO, SOLICITAR_DOCUMENTO, AGORA, "certidão vencida"),
                new Transicao(PENDENTE_DOCUMENTO, EM_ANALISE, RECEBER_DOCUMENTO, AGORA, null)),
                processo.novasTransicoes().subList(1, 3));
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void reprovarSoValeEmAnaliseELevaAEstadoFinal(TipoProcesso tipo) {
        ProcessoPagamento processo = emAnalise(tipo);

        processo.executar(REPROVAR, AGORA, "favorecido divergente");

        assertEquals(REPROVADO, processo.estado());
        assertFalse(processo.ativo());
        assertTrue(processo.acoesDisponiveis().isEmpty());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            invalida(processo, acao);
        }
        invalida(ProcessoPagamento.novo(tipo, COTA, VALOR), REPROVAR);
        invalida(aprovado(tipo), REPROVAR);
    }

    @ParameterizedTest
    @EnumSource(value = AcaoProcesso.class, names = {"SOLICITAR_DOCUMENTO", "REPROVAR", "DEVOLVER_PARA_ANALISE",
            "CANCELAR"})
    void acaoQueExigeMotivoERecusadaSemEle(AcaoProcesso acao) {
        assertTrue(acao.exigeMotivo());
        for (String motivo : new String[] {null, "", "   \t "}) {
            ProcessoPagamento processo = aprovadoOuEmAnalise(acao);
            EstadoProcesso antes = processo.estado();
            int transicoes = processo.novasTransicoes().size();

            assertThrows(DominioException.class, () -> processo.executar(acao, AGORA, motivo));

            assertEquals(antes, processo.estado(), "recusa não muda o estado");
            assertEquals(transicoes, processo.novasTransicoes().size(), "recusa não entra no histórico");
        }
    }

    @Test
    void soEstasAcoesExigemMotivo() {
        Set<AcaoProcesso> exigem = Set.of(SOLICITAR_DOCUMENTO, REPROVAR, DEVOLVER_PARA_ANALISE, CANCELAR);
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            assertEquals(exigem.contains(acao), acao.exigeMotivo(), acao.name());
        }
    }

    @Test
    void motivoFicaNoHistoricoSemOsEspacosDasPontas() {
        ProcessoPagamento processo = emAnalise(TipoProcesso.SERVICO);

        processo.executar(CANCELAR, AGORA, "  desistência da cota  ");

        assertEquals("desistência da cota", processo.novasTransicoes().getLast().observacao());
    }

    @Test
    void motivoComMaisDe500CaracteresERecusado() {
        ProcessoPagamento processo = emAnalise(TipoProcesso.SERVICO);

        assertThrows(DominioException.class, () -> processo.executar(CANCELAR, AGORA, "x".repeat(501)));
        processo.executar(CANCELAR, AGORA, "x".repeat(500));
        assertEquals(CANCELADO, processo.estado());
    }

    @Test
    void acaoForaDeOrdemContinuaSendoTransicaoInvalidaMesmoSemMotivo() {
        ProcessoPagamento aberto = ProcessoPagamento.novo(TipoProcesso.SERVICO, COTA, VALOR);

        assertThrows(TransicaoInvalidaException.class, () -> aberto.executar(REPROVAR, AGORA, null));
    }

    private static ProcessoPagamento emAnalise(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA, VALOR);
        processo.executar(INICIAR_ANALISE, AGORA, null);
        return processo;
    }

    /** Um processo no estado em que a ação vale (devolver vale em aprovado; as demais, em análise). */
    private static ProcessoPagamento aprovadoOuEmAnalise(AcaoProcesso acao) {
        return acao == DEVOLVER_PARA_ANALISE ? aprovado(TipoProcesso.SERVICO) : emAnalise(TipoProcesso.SERVICO);
    }

    private static void invalida(ProcessoPagamento processo, AcaoProcesso acao) {
        assertThrows(TransicaoInvalidaException.class, () -> processo.executar(acao, AGORA, null));
    }

    private static ProcessoPagamento aprovado(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, COTA, VALOR);
        processo.executar(INICIAR_ANALISE, AGORA, null);
        processo.executar(CONCLUIR_ANALISE, AGORA, null);
        if (processo.acoesDisponiveis().contains(CONCLUIR_VISTORIA)) {
            processo.executar(CONCLUIR_VISTORIA, AGORA, null);
        }
        return processo;
    }
}
