package br.com.claudio.consorcio.domain.processo.tipo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.APROVAR_NA_ALCADA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.REPROVAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_ALCADA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.CANCELADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.REPROVADO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** R16 nas tabelas reais dos dois tipos: a linha condicional, o limite exato e as ações de Aguardando alçada. */
class AlcadaPorTipoTest {

    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");
    private static final FluxoProcesso SERVICO = TipoProcesso.SERVICO.fluxo();
    private static final FluxoProcesso IMOBILIARIO = TipoProcesso.IMOBILIARIO.fluxo();

    private static ContextoDaTransicao valor(String valor) {
        return new ContextoDaTransicao(Dinheiro.de(valor), ALCADA);
    }

    @Test
    void servicoConcluirAnaliseDecidePelaAlcada() {
        assertEquals(APROVADO, SERVICO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("30000.00")));
        assertEquals(APROVADO, SERVICO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("50000.00")), "limite exato, inclusive");
        assertEquals(AGUARDANDO_ALCADA, SERVICO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("50000.01")), "um centavo acima");
        assertEquals(AGUARDANDO_ALCADA, SERVICO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("250000.00")));
    }

    @Test
    void imobiliarioConcluirAnaliseVaiSempreParaAVistoria() {
        assertEquals(AGUARDANDO_VISTORIA, IMOBILIARIO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("30000.00")));
        assertEquals(AGUARDANDO_VISTORIA, IMOBILIARIO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("50000.01")));
        assertEquals(AGUARDANDO_VISTORIA, IMOBILIARIO.proximo(EM_ANALISE, CONCLUIR_ANALISE, valor("250000.00")));
    }

    @Test
    void imobiliarioConcluirVistoriaDecidePelaAlcada() {
        assertEquals(APROVADO, IMOBILIARIO.proximo(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, valor("30000.00")));
        assertEquals(APROVADO, IMOBILIARIO.proximo(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, valor("50000.00")), "limite exato, inclusive");
        assertEquals(AGUARDANDO_ALCADA, IMOBILIARIO.proximo(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, valor("50000.01")),
                "um centavo acima");
    }

    @Test
    void asLinhasCondicionaisContinuamNasAcoesDisponiveis() {
        assertTrue(SERVICO.acoesDisponiveis(EM_ANALISE).contains(CONCLUIR_ANALISE));
        assertTrue(IMOBILIARIO.acoesDisponiveis(EM_ANALISE).contains(CONCLUIR_ANALISE));
        assertTrue(IMOBILIARIO.acoesDisponiveis(AGUARDANDO_VISTORIA).contains(CONCLUIR_VISTORIA));
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void aguardandoAlcadaOfereceQuatroAcoesEAsDecidem(TipoProcesso tipo) {
        FluxoProcesso fluxo = tipo.fluxo();
        ContextoDaTransicao acima = valor("80000.00");

        assertEquals(Set.of(APROVAR_NA_ALCADA, REPROVAR, DEVOLVER_PARA_ANALISE, CANCELAR),
                fluxo.acoesDisponiveis(AGUARDANDO_ALCADA));
        assertEquals(APROVADO, fluxo.proximo(AGUARDANDO_ALCADA, APROVAR_NA_ALCADA, acima));
        assertEquals(REPROVADO, fluxo.proximo(AGUARDANDO_ALCADA, REPROVAR, acima));
        assertEquals(EM_ANALISE, fluxo.proximo(AGUARDANDO_ALCADA, DEVOLVER_PARA_ANALISE, acima));
        assertEquals(CANCELADO, fluxo.proximo(AGUARDANDO_ALCADA, CANCELAR, acima));
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void aguardandoAlcadaRecusaAsDemaisAcoes(TipoProcesso tipo) {
        Set<AcaoProcesso> aceitas = Set.of(APROVAR_NA_ALCADA, REPROVAR, DEVOLVER_PARA_ANALISE, CANCELAR);
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            if (!aceitas.contains(acao)) {
                assertThrows(TransicaoInvalidaException.class,
                        () -> tipo.fluxo().proximo(AGUARDANDO_ALCADA, acao, valor("80000.00")), acao.name());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(TipoProcesso.class)
    void aprovarNaAlcadaSoValeEmAguardandoAlcada(TipoProcesso tipo) {
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            assertEquals(estado == AGUARDANDO_ALCADA, tipo.fluxo().acoesDisponiveis(estado).contains(APROVAR_NA_ALCADA),
                    estado.name());
        }
    }
}
