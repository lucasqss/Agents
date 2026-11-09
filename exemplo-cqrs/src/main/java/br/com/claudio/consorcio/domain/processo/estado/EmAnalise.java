package br.com.claudio.consorcio.domain.processo.estado;

import br.com.claudio.consorcio.domain.processo.FluxoProcesso;

/**
 * Padrão: State, estado concreto [GoF].
 * Em análise; o próximo estado vem do fluxo do tipo (Strategy).
 */
public record EmAnalise() implements EstadoProcesso {

    public static final String NOME = "EM_ANALISE";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso concluirAnalise(FluxoProcesso fluxo) {
        return fluxo.aposAnalise();
    }
}
