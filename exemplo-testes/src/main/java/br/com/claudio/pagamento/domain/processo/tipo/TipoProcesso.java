package br.com.claudio.pagamento.domain.processo.tipo;

import br.com.claudio.pagamento.domain.processo.fluxo.FluxoProcesso;

/**
 * Tipos de processo. O nome da constante é o código gravado na coluna {@code tipo}.
 * Tipo novo: uma constante aqui e o {@code Fluxo<Tipo>} com os desvios dele.
 */
public enum TipoProcesso {

    IMOBILIARIO(FluxoImobiliario.definir()),
    SERVICO(FluxoServico.definir());

    // FluxoProcesso é imutável (tabela fechada em mapas não modificáveis), mas o ErrorProne não tem como saber disso.
    @SuppressWarnings("ImmutableEnumChecker")
    private final FluxoProcesso fluxo;

    TipoProcesso(FluxoProcesso fluxo) {
        this.fluxo = fluxo;
    }

    public FluxoProcesso fluxo() {
        return fluxo;
    }
}
