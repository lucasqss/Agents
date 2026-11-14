package br.com.claudio.consorcio.domain.processo.fluxo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.ALTERAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCORDAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DISCORDAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONFIRMAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.RECUSAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.SOLICITAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.TENTAR_PAGAMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.PROPOR_ALTERACAO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.RECEBER_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.REPROVAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.SOLICITAR_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.ABERTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_ALCADA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_CONCORDANCIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.CANCELADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGAMENTO_SOLICITADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PAGO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.PENDENTE_DOCUMENTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.REPROVADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class FluxoProcessoTest {

    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");
    private static final ContextoDaTransicao ATE_A_ALCADA = new ContextoDaTransicao(Dinheiro.de("30000.00"), ALCADA);
    private static final ContextoDaTransicao ACIMA_DA_ALCADA = new ContextoDaTransicao(Dinheiro.de("80000.00"), ALCADA);

    private final FluxoProcesso comum = FluxoProcesso.comum().construir();

    @Test
    void caminhoComum() {
        assertEquals(EM_ANALISE, comum.proximo(ABERTO, INICIAR_ANALISE, ATE_A_ALCADA));
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, ATE_A_ALCADA));
        assertEquals(PAGAMENTO_SOLICITADO, comum.proximo(APROVADO, SOLICITAR_PAGAMENTO, ATE_A_ALCADA));
        assertEquals(PAGO, comum.proximo(PAGAMENTO_SOLICITADO, CONFIRMAR_PAGAMENTO, ATE_A_ALCADA));
    }

    @Test
    void acaoSemLinhaNaTabelaEInvalida() {
        TransicaoInvalidaException erro =
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(ABERTO, SOLICITAR_PAGAMENTO, ATE_A_ALCADA));
        assertEquals("Não é possível solicitar o pagamento de um processo no estado ABERTO", erro.getMessage());
        assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(EM_ANALISE, CONCLUIR_VISTORIA, ATE_A_ALCADA));
    }

    @ParameterizedTest
    @EnumSource(EstadoProcesso.class)
    void cancelaEmTodoEstadoNaoFinalESoNeles(EstadoProcesso estado) {
        if (estado.finalizado()) {
            assertTrue(comum.acoesDisponiveis(estado).isEmpty());
            assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(estado, CANCELAR, ATE_A_ALCADA));
        } else if (estado == PAGAMENTO_SOLICITADO) {
            // Exceção declarada na tabela (semCancelamentoEm): só a rotina tira o processo daqui.
            assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(estado, CANCELAR, ATE_A_ALCADA));
        } else {
            assertEquals(CANCELADO, comum.proximo(estado, CANCELAR, ATE_A_ALCADA));
        }
    }

    @Test
    void pagamentoSolicitadoSoAceitaOsResultadosDaRotina() {
        assertFalse(PAGAMENTO_SOLICITADO.finalizado());
        assertEquals(PAGO, comum.proximo(PAGAMENTO_SOLICITADO, CONFIRMAR_PAGAMENTO, ATE_A_ALCADA));
        assertEquals(APROVADO, comum.proximo(PAGAMENTO_SOLICITADO, RECUSAR_PAGAMENTO, ATE_A_ALCADA));
        assertEquals(PAGAMENTO_SOLICITADO, comum.proximo(PAGAMENTO_SOLICITADO, TENTAR_PAGAMENTO, ATE_A_ALCADA));
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            if (!acao.daRotina()) {
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(PAGAMENTO_SOLICITADO, acao, ATE_A_ALCADA),
                        acao.name());
            }
        }
    }

    @Test
    void asAcoesDaRotinaNaoSaoOferecidasAPessoaNemExecutaveisPeloGenerico() {
        assertEquals(Set.of(CONFIRMAR_PAGAMENTO, RECUSAR_PAGAMENTO, TENTAR_PAGAMENTO),
                java.util.Arrays.stream(AcaoProcesso.values()).filter(AcaoProcesso::daRotina)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(comum.acoesDisponiveis(PAGAMENTO_SOLICITADO).isEmpty());
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            if (acao.daRotina()) {
                assertTrue(acao.comMetodoProprio(), acao.name());
                for (EstadoProcesso estado : EstadoProcesso.values()) {
                    assertFalse(comum.acoesDisponiveis(estado).contains(acao), acao + " em " + estado);
                }
            }
        }
    }

    @Test
    void solicitarPagamentoEOferecidoEmAprovadoELevaPagamentoSolicitadoComMetodoProprio() {
        assertTrue(SOLICITAR_PAGAMENTO.comMetodoProprio());
        assertFalse(SOLICITAR_PAGAMENTO.daRotina());
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            assertEquals(estado == APROVADO, comum.acoesDisponiveis(estado).contains(SOLICITAR_PAGAMENTO), estado.name());
        }
    }

    @Test
    void umTipoTrocaEAcrescentaLinhasSemAfetarOComum() {
        FluxoProcesso comVistoria = FluxoProcesso.comum()
                .transicao(EM_ANALISE, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA, Papel.BACKOFFICE)
                .transicao(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, APROVADO, Papel.BACKOFFICE)
                .construir();

        assertEquals(AGUARDANDO_VISTORIA, comVistoria.proximo(EM_ANALISE, CONCLUIR_ANALISE, ATE_A_ALCADA));
        assertEquals(APROVADO, comVistoria.proximo(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, ATE_A_ALCADA));
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, ATE_A_ALCADA));
    }

    @Test
    void documentoEReprovacaoNaAnalise() {
        assertEquals(PENDENTE_DOCUMENTO, comum.proximo(EM_ANALISE, SOLICITAR_DOCUMENTO, ATE_A_ALCADA));
        assertEquals(EM_ANALISE, comum.proximo(PENDENTE_DOCUMENTO, RECEBER_DOCUMENTO, ATE_A_ALCADA));
        assertEquals(REPROVADO, comum.proximo(EM_ANALISE, REPROVAR, ATE_A_ALCADA));
        assertEquals(Set.of(SOLICITAR_DOCUMENTO, REPROVAR, CONCLUIR_ANALISE, PROPOR_ALTERACAO, CANCELAR),
                comum.acoesDisponiveis(EM_ANALISE));
        assertEquals(Set.of(RECEBER_DOCUMENTO, CANCELAR), comum.acoesDisponiveis(PENDENTE_DOCUMENTO));
    }

    @Test
    void reprovadoEFinalESoReprovaDaAnaliseEDaAlcada() {
        assertTrue(REPROVADO.finalizado());
        assertTrue(comum.acoesDisponiveis(REPROVADO).isEmpty());
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            if (estado != EM_ANALISE && estado != AGUARDANDO_ALCADA) {
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(estado, REPROVAR, ATE_A_ALCADA), estado.name());
            }
        }
    }

    @Test
    void acoesDisponiveisSaemDaTabela() {
        assertEquals(Set.of(INICIAR_ANALISE, ALTERAR, CANCELAR), comum.acoesDisponiveis(ABERTO));
        assertEquals(Set.of(DEVOLVER_PARA_ANALISE, SOLICITAR_PAGAMENTO, CANCELAR), comum.acoesDisponiveis(APROVADO));
    }

    @Test
    void estadoFinalNaoTemSaida() {
        assertThrows(IllegalArgumentException.class,
                () -> FluxoProcesso.comum().transicao(PAGO, INICIAR_ANALISE, EM_ANALISE));
    }

    @Test
    void alterarVaiDeAbertoParaAberto() {
        assertEquals(ABERTO, comum.proximo(ABERTO, ALTERAR, ATE_A_ALCADA));
    }

    @Test
    void proporAlteracaoVaiDeAnaliseParaAguardandoConcordancia() {
        assertEquals(AGUARDANDO_CONCORDANCIA, comum.proximo(EM_ANALISE, PROPOR_ALTERACAO, ATE_A_ALCADA));
    }

    @Test
    void concordarEDiscordarVoltamParaAnalise() {
        assertEquals(EM_ANALISE, comum.proximo(AGUARDANDO_CONCORDANCIA, CONCORDAR, ATE_A_ALCADA));
        assertEquals(EM_ANALISE, comum.proximo(AGUARDANDO_CONCORDANCIA, DISCORDAR, ATE_A_ALCADA));
    }

    @Test
    void aguardandoConcordanciaSoAceitaConcordarDiscordarECancelar() {
        assertFalse(AGUARDANDO_CONCORDANCIA.finalizado());
        assertEquals(Set.of(CONCORDAR, DISCORDAR, CANCELAR), comum.acoesDisponiveis(AGUARDANDO_CONCORDANCIA));
        assertEquals(CANCELADO, comum.proximo(AGUARDANDO_CONCORDANCIA, CANCELAR, ATE_A_ALCADA));
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            if (!Set.of(CONCORDAR, DISCORDAR, CANCELAR).contains(acao)) {
                assertThrows(TransicaoInvalidaException.class, () -> comum.proximo(AGUARDANDO_CONCORDANCIA, acao, ATE_A_ALCADA),
                        acao.name());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(value = AcaoProcesso.class, names = {"ALTERAR", "PROPOR_ALTERACAO", "CONCORDAR", "DISCORDAR"})
    void asAcoesDeAlteracaoSoValemOndeATabelaDiz(AcaoProcesso acao) {
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            boolean vale = comum.acoesDisponiveis(estado).contains(acao);
            boolean esperado = switch (acao) {
                case ALTERAR -> estado == ABERTO;
                case PROPOR_ALTERACAO -> estado == EM_ANALISE;
                default -> estado == AGUARDANDO_CONCORDANCIA;
            };
            assertEquals(esperado, vale, acao + " em " + estado);
        }
    }

    @Test
    void motivoEObrigatorioEmProporEDiscordarMasNaoEmAlterarEConcordar() {
        assertTrue(PROPOR_ALTERACAO.exigeMotivo());
        assertTrue(DISCORDAR.exigeMotivo());
        assertFalse(ALTERAR.exigeMotivo());
        assertFalse(CONCORDAR.exigeMotivo());
    }

    private static ContextoDaTransicao comValor(String valor) {
        return new ContextoDaTransicao(Dinheiro.de(valor), ALCADA);
    }

    @Test
    void concluirAnaliseDecidePelaAlcada() {
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, comValor("50000.00")), "no limite exato");
        assertEquals(AGUARDANDO_ALCADA, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, comValor("50000.01")), "um centavo acima");
        assertEquals(APROVADO, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, comValor("0.01")));
        assertEquals(AGUARDANDO_ALCADA, comum.proximo(EM_ANALISE, CONCLUIR_ANALISE, ACIMA_DA_ALCADA));
    }

    @Test
    void linhaCondicionalPodeSerDeclaradaComQualquerCondicao() {
        FluxoProcesso fluxo = FluxoProcesso.comum()
                .transicaoCondicional(ABERTO, INICIAR_ANALISE, contexto -> false, REPROVADO, PENDENTE_DOCUMENTO, Papel.BACKOFFICE)
                .construir();

        assertEquals(PENDENTE_DOCUMENTO, fluxo.proximo(ABERTO, INICIAR_ANALISE, ATE_A_ALCADA));
        assertEquals(Set.of(INICIAR_ANALISE, ALTERAR, CANCELAR), fluxo.acoesDisponiveis(ABERTO));
    }

    @Test
    void condicaoDaAlcadaExigeAAlcada() {
        assertThrows(IllegalStateException.class, () -> comum.proximo(EM_ANALISE, CONCLUIR_ANALISE,
                new ContextoDaTransicao(Dinheiro.de("10.00"), null)));
    }

    @Test
    void estadoFinalNaoTemLinhaCondicional() {
        assertThrows(IllegalArgumentException.class, () -> FluxoProcesso.comum()
                .transicaoCondicional(PAGO, CANCELAR, contexto -> true, APROVADO, REPROVADO));
    }
}
