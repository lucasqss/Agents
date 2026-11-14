package br.com.claudio.consorcio.domain.processo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONFIRMAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.RECUSAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.SOLICITAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.TENTAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGAMENTO_SOLICITADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Pagamento como saga (ADR 002): solicitar só grava; a rotina aplica o resultado da tesouraria. */
class PagamentoComoSagaTest {

    private static final Dinheiro VALOR = Dinheiro.de("30000.00");
    private static final Instant AGORA = Instant.parse("2026-11-13T10:00:00Z");
    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");
    private static final IdentificacaoSolicitacao PRIMEIRA = new IdentificacaoSolicitacao(UUID.randomUUID());
    private static final IdentificacaoSolicitacao SEGUNDA = new IdentificacaoSolicitacao(UUID.randomUUID());

    private static ProcessoPagamento aprovado(TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, 1L, VALOR, List.of(
                new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", VALOR)));
        processo.executar(AcaoProcesso.INICIAR_ANALISE, AGORA, null, ALCADA);
        processo.executar(AcaoProcesso.CONCLUIR_ANALISE, AGORA, null, ALCADA);
        if (tipo == TipoProcesso.IMOBILIARIO) {
            processo.executar(AcaoProcesso.CONCLUIR_VISTORIA, AGORA, null, ALCADA);
        }
        return processo;
    }

    private static ProcessoPagamento solicitado(TipoProcesso tipo) {
        ProcessoPagamento processo = aprovado(tipo);
        processo.solicitarPagamento(PRIMEIRA, AGORA, null);
        return processo;
    }

    // ---- Solicitar ----

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void solicitarSoLevaAPagamentoSolicitadoEGuardaAIdentificacao(TipoProcesso tipo) {
        ProcessoPagamento processo = aprovado(tipo);

        processo.solicitarPagamento(PRIMEIRA, AGORA, "pedido do cliente");

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
        assertNull(processo.protocoloPagamento());
        assertTrue(processo.ativo());
        assertEquals(new Transicao(APROVADO, PAGAMENTO_SOLICITADO, SOLICITAR_PAGAMENTO, AGORA, "pedido do cliente"),
                processo.novasTransicoes().getLast());
    }

    @Test
    void solicitarExigeIdentificacaoEAprovado() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);
        assertThrows(DominioException.class, () -> processo.solicitarPagamento(null, AGORA, null));
        assertEquals(APROVADO, processo.estado());

        ProcessoPagamento aberto = ProcessoPagamento.novo(TipoProcesso.SERVICO, 1L, VALOR, List.of(
                new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", VALOR)));
        assertThrows(TransicaoInvalidaException.class, () -> aberto.solicitarPagamento(PRIMEIRA, AGORA, null));
        assertTrue(aberto.identificacaoSolicitacao().isEmpty());
    }

    @Test
    void solicitarDuasVezesERecusado() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        assertThrows(TransicaoInvalidaException.class, () -> processo.solicitarPagamento(SEGUNDA, AGORA, null));
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
    }

    @Test
    void aIdentificacaoExigeUmUuid() {
        assertThrows(DominioException.class, () -> new IdentificacaoSolicitacao(null));
        UUID uuid = UUID.randomUUID();
        assertEquals(uuid.toString(), new IdentificacaoSolicitacao(uuid).texto());
    }

    // ---- Em pagamento solicitado, só a rotina age ----

    @ParameterizedTest
    @EnumSource(AcaoProcesso.class)
    void nenhumaAcaoDePessoaValeEmPagamentoSolicitado(AcaoProcesso acao) {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        assertThrows(DominioException.class, () -> processo.executar(acao, AGORA, "motivo", ALCADA), acao.name());

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertTrue(processo.acoesDisponiveis().isEmpty());
        assertEquals(1, processo.novasTransicoes().stream().filter(t -> t.acao() == SOLICITAR_PAGAMENTO).count());
        assertFalse(processo.novasTransicoes().stream().anyMatch(t -> t.acao() == CANCELAR));
    }

    // ---- Confirmar ----

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void confirmarPagaGuardaOProtocoloEDescartaAIdentificacao(TipoProcesso tipo) {
        ProcessoPagamento processo = solicitado(tipo);

        processo.confirmarPagamento("  778899 ", AGORA.plusSeconds(5));

        assertEquals(PAGO, processo.estado());
        assertEquals("778899", processo.protocoloPagamento());
        assertTrue(processo.identificacaoSolicitacao().isEmpty());
        assertFalse(processo.ativo());
        Transicao registro = processo.novasTransicoes().getLast();
        assertEquals(CONFIRMAR_PAGAMENTO, registro.acao());
        assertEquals(PAGO, registro.para());
        assertEquals(PAGAMENTO_SOLICITADO, registro.de());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void confirmarExigeProtocolo(String protocolo) {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        int transicoes = processo.novasTransicoes().size();

        assertThrows(DominioException.class, () -> processo.confirmarPagamento(protocolo, AGORA));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertNull(processo.protocoloPagamento());
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
        assertEquals(transicoes, processo.novasTransicoes().size());
    }

    @Test
    void confirmarForaDePagamentoSolicitadoERecusado() {
        ProcessoPagamento processo = aprovado(TipoProcesso.SERVICO);

        assertThrows(TransicaoInvalidaException.class, () -> processo.confirmarPagamento("778899", AGORA));
        assertEquals(APROVADO, processo.estado());
        assertNull(processo.protocoloPagamento());
    }

    // ---- Recusar ----

    @Test
    void recusarVoltaAAprovadoComOMotivoEDescartaAIdentificacao() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        processo.recusarPagamento("conta inválida", AGORA);

        assertEquals(APROVADO, processo.estado());
        assertTrue(processo.identificacaoSolicitacao().isEmpty());
        assertNull(processo.protocoloPagamento());
        assertEquals(new Transicao(PAGAMENTO_SOLICITADO, APROVADO, RECUSAR_PAGAMENTO, AGORA, "conta inválida"),
                processo.novasTransicoes().getLast());
        assertTrue(processo.acoesDisponiveis().contains(SOLICITAR_PAGAMENTO));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void recusarExigeMotivo(String motivo) {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        assertThrows(DominioException.class, () -> processo.recusarPagamento(motivo, AGORA));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
    }

    @Test
    void motivoMuitoLongoDaTesourariaFicaNoHistoricoSemRecusarOResultado() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        processo.recusarPagamento("x".repeat(900), AGORA);

        assertEquals(APROVADO, processo.estado());
        assertEquals(Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO, processo.novasTransicoes().getLast().observacao().length());
    }

    @Test
    void novaSolicitacaoDepoisDaRecusaTemIdentificacaoNova() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        processo.recusarPagamento("conta inválida", AGORA);

        processo.solicitarPagamento(SEGUNDA, AGORA.plusSeconds(60), null);

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(SEGUNDA, processo.identificacaoSolicitacao().orElseThrow());
        assertFalse(processo.aguardaResultadoDe(PRIMEIRA));
        assertTrue(processo.aguardaResultadoDe(SEGUNDA));
    }

    // ---- Tentativa sem resultado ----

    @Test
    void tentativaSemResultadoContinuaEsperandoComAMesmaIdentificacao() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        processo.registrarTentativaSemResultado("Tesouraria sem resposta: TimeoutException", AGORA);
        processo.registrarTentativaSemResultado("Tesouraria sem resposta: TimeoutException", AGORA.plusSeconds(300));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
        assertNull(processo.protocoloPagamento());
        List<Transicao> tentativas = processo.novasTransicoes().stream().filter(t -> t.acao() == TENTAR_PAGAMENTO).toList();
        assertEquals(2, tentativas.size());
        assertEquals(PAGAMENTO_SOLICITADO, tentativas.getFirst().de());
        assertEquals(PAGAMENTO_SOLICITADO, tentativas.getFirst().para());
        assertEquals("Tesouraria sem resposta: TimeoutException", tentativas.getFirst().observacao());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void tentativaExigeObservacao(String observacao) {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        int transicoes = processo.novasTransicoes().size();

        assertThrows(DominioException.class, () -> processo.registrarTentativaSemResultado(observacao, AGORA));

        assertEquals(transicoes, processo.novasTransicoes().size());
    }

    // ---- Aplicar o resultado da tesouraria (R20, R21, R22, R31) ----

    @Test
    void aplicarConfirmadoPagaOProcesso() {
        ProcessoPagamento processo = solicitado(TipoProcesso.IMOBILIARIO);

        boolean aplicou = processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.confirmado("778899"), AGORA);

        assertTrue(aplicou);
        assertEquals(PAGO, processo.estado());
        assertEquals("778899", processo.protocoloPagamento());
    }

    @Test
    void aplicarRecusadoVoltaAAprovado() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        assertTrue(processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.recusado("conta inválida"), AGORA));

        assertEquals(APROVADO, processo.estado());
        assertEquals("conta inválida", processo.novasTransicoes().getLast().observacao());
    }

    @Test
    void aplicarSemRespostaEIncoerenteContinuamEsperandoENoHistorico() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);

        assertTrue(processo.aplicarResultado(PRIMEIRA, new ResultadoPagamento.SemResposta("fora do ar"), AGORA));
        assertTrue(processo.aplicarResultado(PRIMEIRA, new ResultadoPagamento.Incoerente("sem protocolo"), AGORA));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(PRIMEIRA, processo.identificacaoSolicitacao().orElseThrow());
        List<Transicao> tentativas = processo.novasTransicoes().stream().filter(t -> t.acao() == TENTAR_PAGAMENTO).toList();
        assertEquals(List.of("fora do ar", "sem protocolo"), tentativas.stream().map(Transicao::observacao).toList());
    }

    @Test
    void confirmacaoSemProtocoloERecusaSemMotivoViramIncoerentes() {
        assertInstanceOfIncoerente(ResultadoPagamento.confirmado(null));
        assertInstanceOfIncoerente(ResultadoPagamento.confirmado("  "));
        assertInstanceOfIncoerente(ResultadoPagamento.recusado(null));
        assertInstanceOfIncoerente(ResultadoPagamento.recusado(""));

        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        assertTrue(processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.confirmado(null), AGORA));
        assertTrue(processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.recusado(" "), AGORA));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertNull(processo.protocoloPagamento());
        assertEquals(2, processo.novasTransicoes().stream().filter(t -> t.acao() == TENTAR_PAGAMENTO).count());
        assertFalse(processo.novasTransicoes().stream().anyMatch(t -> t.acao() == CONFIRMAR_PAGAMENTO
                || t.acao() == RECUSAR_PAGAMENTO));
    }

    @Test
    void resultadoComDadoValidoGuardaOTextoSemEspacosDasPontas() {
        assertEquals(new ResultadoPagamento.Confirmado("778899"), ResultadoPagamento.confirmado(" 778899 "));
        assertEquals(new ResultadoPagamento.Recusado("conta inválida"), ResultadoPagamento.recusado(" conta inválida "));
        assertThrows(DominioException.class, () -> new ResultadoPagamento.Confirmado(" "));
        assertThrows(DominioException.class, () -> new ResultadoPagamento.Recusado(null));
        assertThrows(DominioException.class, () -> new ResultadoPagamento.SemResposta(""));
        assertThrows(DominioException.class, () -> new ResultadoPagamento.Incoerente(null));
    }

    @Test
    void aplicarDepoisDeOutraExecucaoTerAplicadoNaoFazNada() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.confirmado("778899"), AGORA);
        int transicoes = processo.novasTransicoes().size();

        assertFalse(processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.confirmado("OUTRO"), AGORA));
        assertFalse(processo.aplicarResultado(PRIMEIRA, new ResultadoPagamento.SemResposta("fora do ar"), AGORA));

        assertEquals("778899", processo.protocoloPagamento());
        assertEquals(transicoes, processo.novasTransicoes().size());
    }

    @Test
    void resultadoDeOutraSolicitacaoNaoEAplicado() {
        ProcessoPagamento processo = solicitado(TipoProcesso.SERVICO);
        processo.recusarPagamento("conta inválida", AGORA);
        processo.solicitarPagamento(SEGUNDA, AGORA, null);
        int transicoes = processo.novasTransicoes().size();

        assertFalse(processo.aplicarResultado(PRIMEIRA, ResultadoPagamento.confirmado("778899"), AGORA));

        assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(SEGUNDA, processo.identificacaoSolicitacao().orElseThrow());
        assertEquals(transicoes, processo.novasTransicoes().size());
    }

    // ---- Reconstituir ----

    @Test
    void reconstituirExigeIdentificacaoSeESomenteSeEmPagamentoSolicitado() {
        List<Favorecido> favorecidos = List.of(new Favorecido("F", Documento.de("12345678901"), "vendedor", VALOR));
        assertThrows(DominioException.class, () -> ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, VALOR,
                favorecidos, PAGAMENTO_SOLICITADO, null, null, null, 0));
        assertThrows(DominioException.class, () -> ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, VALOR,
                favorecidos, APROVADO, null, null, PRIMEIRA, 0));

        ProcessoPagamento lido = ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, VALOR, favorecidos,
                PAGAMENTO_SOLICITADO, null, null, PRIMEIRA, 4);
        assertEquals(PRIMEIRA, lido.identificacaoSolicitacao().orElseThrow());
        assertTrue(lido.novasTransicoes().isEmpty());
    }

    @Test
    void processoPagoAntesDestaFatiaContinuaValido() {
        List<Favorecido> favorecidos = List.of(new Favorecido("F", Documento.de("12345678901"), "vendedor", VALOR));

        ProcessoPagamento antigo = ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, VALOR, favorecidos, PAGO,
                "TES-1", null, null, 3);

        assertEquals(PAGO, antigo.estado());
        assertEquals("TES-1", antigo.protocoloPagamento());
        assertTrue(antigo.identificacaoSolicitacao().isEmpty());
    }

    private static void assertInstanceOfIncoerente(ResultadoPagamento resultado) {
        assertTrue(resultado instanceof ResultadoPagamento.Incoerente, resultado.toString());
    }
}
