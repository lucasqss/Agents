package br.com.claudio.consorcio.domain.processo.tipo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.CondicaoDaTransicao.VALOR_PASSA_DA_ALCADA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_ALCADA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;

import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;

/**
 * Imobiliário: depois da análise, passa pela vistoria antes de ser aprovado; a vistoria pode devolver para análise.
 * Concluir a análise leva sempre à vistoria; é concluir a vistoria que decide entre Aprovado e Aguardando alçada (R16).
 */
final class FluxoImobiliario {

    private FluxoImobiliario() {
    }

    static FluxoProcesso definir() {
        return FluxoProcesso.comum()
                .transicao(EM_ANALISE, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA)
                .transicaoCondicional(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, VALOR_PASSA_DA_ALCADA, AGUARDANDO_ALCADA, APROVADO)
                .transicao(AGUARDANDO_VISTORIA, DEVOLVER_PARA_ANALISE, EM_ANALISE)
                .construir();
    }
}
