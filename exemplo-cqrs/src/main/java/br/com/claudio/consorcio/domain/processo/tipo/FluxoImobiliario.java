package br.com.claudio.consorcio.domain.processo.tipo;

import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.APROVADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;

import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;

/** Imobiliário: depois da análise, passa pela vistoria antes de ser aprovado; a vistoria pode devolver para análise. */
final class FluxoImobiliario {

    private FluxoImobiliario() {
    }

    static FluxoProcesso definir() {
        return FluxoProcesso.comum()
                .transicao(EM_ANALISE, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA)
                .transicao(AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, APROVADO)
                .transicao(AGUARDANDO_VISTORIA, DEVOLVER_PARA_ANALISE, EM_ANALISE)
                .construir();
    }
}
