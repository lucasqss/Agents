package br.com.claudio.consorcio.domain.processo.tipo;

import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;

/** Serviço: segue o caminho comum, sem desvios. */
final class FluxoServico {

    private FluxoServico() {
    }

    static FluxoProcesso definir() {
        return FluxoProcesso.comum().construir();
    }
}
