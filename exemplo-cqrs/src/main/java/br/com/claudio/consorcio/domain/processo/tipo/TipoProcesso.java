package br.com.claudio.consorcio.domain.processo.tipo;

import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;

/**
 * Tipos de processo. O nome da constante é o código gravado na coluna {@code tipo}.
 * Tipo novo: uma constante aqui, o {@code Fluxo<Tipo>} com os desvios dele e o rótulo em {@code Rotulos}.
 */
public enum TipoProcesso {

    IMOBILIARIO(FluxoImobiliario.definir()),
    SERVICO(FluxoServico.definir());

    private final FluxoProcesso fluxo;

    TipoProcesso(FluxoProcesso fluxo) {
        this.fluxo = fluxo;
    }

    public FluxoProcesso fluxo() {
        return fluxo;
    }
}
