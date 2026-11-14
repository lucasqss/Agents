package br.com.claudio.consorcio.domain.processo.fluxo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.*;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.*;
import static br.com.claudio.consorcio.domain.processo.fluxo.Papel.APROVADOR;
import static br.com.claudio.consorcio.domain.processo.fluxo.Papel.BACKOFFICE;
import static br.com.claudio.consorcio.domain.processo.fluxo.Papel.CLIENTE;
import static br.com.claudio.consorcio.domain.processo.fluxo.Papel.TESOURARIA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * R18: quem pode pedir cada linha da tabela do fluxo (quadro "Quem pode executar cada ação"), por situação, nos dois
 * tipos, e a marca da regra da segunda pessoa (R17). Domínio puro, sem Quarkus.
 */
class PapeisDoFluxoTest {

    private static final FluxoProcesso SERVICO = TipoProcesso.SERVICO.fluxo();
    private static final FluxoProcesso IMOBILIARIO = TipoProcesso.IMOBILIARIO.fluxo();

    @Test
    void reprovarMudaDePapelConformeASituacao() {
        assertEquals(Set.of(BACKOFFICE), SERVICO.papeisDe(EM_ANALISE, REPROVAR));
        assertEquals(Set.of(APROVADOR), SERVICO.papeisDe(AGUARDANDO_ALCADA, REPROVAR));
        assertEquals(Set.of(BACKOFFICE), IMOBILIARIO.papeisDe(EM_ANALISE, REPROVAR));
        assertEquals(Set.of(APROVADOR), IMOBILIARIO.papeisDe(AGUARDANDO_ALCADA, REPROVAR));
    }

    @Test
    void devolverParaAnaliseMudaDePapelConformeASituacao() {
        assertEquals(Set.of(BACKOFFICE), SERVICO.papeisDe(APROVADO, DEVOLVER_PARA_ANALISE));
        assertEquals(Set.of(APROVADOR), SERVICO.papeisDe(AGUARDANDO_ALCADA, DEVOLVER_PARA_ANALISE));
        assertEquals(Set.of(BACKOFFICE), IMOBILIARIO.papeisDe(AGUARDANDO_VISTORIA, DEVOLVER_PARA_ANALISE));
        assertEquals(Set.of(APROVADOR), IMOBILIARIO.papeisDe(AGUARDANDO_ALCADA, DEVOLVER_PARA_ANALISE));
    }

    @Test
    void asAcoesDoBackofficeSaoSoDele() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            FluxoProcesso fluxo = tipo.fluxo();
            assertEquals(Set.of(BACKOFFICE), fluxo.papeisDe(ABERTO, INICIAR_ANALISE), tipo.name());
            assertEquals(Set.of(BACKOFFICE), fluxo.papeisDe(EM_ANALISE, SOLICITAR_DOCUMENTO), tipo.name());
            assertEquals(Set.of(BACKOFFICE), fluxo.papeisDe(PENDENTE_DOCUMENTO, RECEBER_DOCUMENTO), tipo.name());
            assertEquals(Set.of(BACKOFFICE), fluxo.papeisDe(EM_ANALISE, CONCLUIR_ANALISE), tipo.name());
            assertEquals(Set.of(BACKOFFICE), fluxo.papeisDe(EM_ANALISE, PROPOR_ALTERACAO), tipo.name());
        }
        assertEquals(Set.of(BACKOFFICE), IMOBILIARIO.papeisDe(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA));
    }

    @Test
    void asAcoesDoClienteSaoSoDele() {
        assertEquals(Set.of(CLIENTE), SERVICO.papeisDe(ABERTO, ALTERAR));
        assertEquals(Set.of(CLIENTE), SERVICO.papeisDe(AGUARDANDO_CONCORDANCIA, CONCORDAR));
        assertEquals(Set.of(CLIENTE), SERVICO.papeisDe(AGUARDANDO_CONCORDANCIA, DISCORDAR));
    }

    @Test
    void aprovarNaAlcadaEDoAprovadorESolicitarPagamentoEDaTesouraria() {
        assertEquals(Set.of(APROVADOR), SERVICO.papeisDe(AGUARDANDO_ALCADA, APROVAR_NA_ALCADA));
        assertEquals(Set.of(APROVADOR), IMOBILIARIO.papeisDe(AGUARDANDO_ALCADA, APROVAR_NA_ALCADA));
        assertEquals(Set.of(TESOURARIA), SERVICO.papeisDe(APROVADO, SOLICITAR_PAGAMENTO));
    }

    @Test
    void cancelarEDoBackofficeEDoAprovadorEmTodaSituacaoQueAceita() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                if (tipo.fluxo().acoesDisponiveis(estado).contains(CANCELAR)) {
                    assertEquals(Set.of(BACKOFFICE, APROVADOR), tipo.fluxo().papeisDe(estado, CANCELAR),
                            tipo + " em " + estado);
                }
            }
        }
    }

    @Test
    void asLinhasDaRotinaNaoTemPapel() {
        assertEquals(Set.of(), SERVICO.papeisDe(PAGAMENTO_SOLICITADO, CONFIRMAR_PAGAMENTO));
        assertEquals(Set.of(), SERVICO.papeisDe(PAGAMENTO_SOLICITADO, RECUSAR_PAGAMENTO));
        assertEquals(Set.of(), SERVICO.papeisDe(PAGAMENTO_SOLICITADO, TENTAR_PAGAMENTO));
    }

    @Test
    void todaAcaoDePessoaDeTodaLinhaTemAoMenosUmPapel() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                for (AcaoProcesso acao : tipo.fluxo().acoesDisponiveis(estado)) {
                    assertFalse(tipo.fluxo().papeisDe(estado, acao).isEmpty(), tipo + " " + estado + " " + acao);
                }
            }
        }
    }

    @Test
    void acaoQueNaoValeNaSituacaoNaoTemPapel() {
        assertThrows(TransicaoInvalidaException.class, () -> SERVICO.papeisDe(APROVADO, REPROVAR));
    }

    @Test
    void soAprovarNaAlcadaExigeOutraPessoaQueConcluiuAAnalise() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                for (AcaoProcesso acao : tipo.fluxo().acoesDisponiveis(estado)) {
                    assertEquals(estado == AGUARDANDO_ALCADA && acao == APROVAR_NA_ALCADA,
                            tipo.fluxo().exigeOutraPessoaQueConcluiuAAnalise(estado, acao), tipo + " " + estado + " " + acao);
                }
            }
        }
        assertFalse(SERVICO.exigeOutraPessoaQueConcluiuAAnalise(APROVADO, REPROVAR), "ação que não vale");
        assertTrue(IMOBILIARIO.exigeOutraPessoaQueConcluiuAAnalise(AGUARDANDO_ALCADA, APROVAR_NA_ALCADA));
    }

    @Test
    void tabelaRecusaAcaoDePessoaSemPapelEAcaoDaRotinaComPapel() {
        assertThrows(IllegalArgumentException.class,
                () -> FluxoProcesso.comum().transicao(ABERTO, INICIAR_ANALISE, EM_ANALISE));
        assertThrows(IllegalArgumentException.class,
                () -> FluxoProcesso.comum().transicao(PAGAMENTO_SOLICITADO, CONFIRMAR_PAGAMENTO, PAGO, BACKOFFICE));
    }
}
