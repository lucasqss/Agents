package br.com.claudio.consorcio.domain.processo;

/**
 * Padrão: Strategy, escolhe a estratégia de cada tipo [GoF].
 * Tipo novo = uma constante aqui + uma implementação de {@link FluxoProcesso}.
 */
public enum TipoProcesso {
    IMOBILIARIO(new FluxoImobiliario()),
    SERVICO(new FluxoServico());

    private final FluxoProcesso fluxo;

    TipoProcesso(FluxoProcesso fluxo) {
        this.fluxo = fluxo;
    }

    public FluxoProcesso fluxo() {
        return fluxo;
    }
}
