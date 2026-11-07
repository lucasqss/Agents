package br.com.claudio.consorcio.command.domain.estado;

import br.com.claudio.consorcio.command.domain.ProcessoPagamento;

public record EmAnalise() implements EstadoProcesso {

    public static final String NOME = "EM_ANALISE";

    @Override
    public String nome() {
        return NOME;
    }

    /** O tipo do processo decide o próximo passo: vistoria (imobiliário) ou aprovação direta (serviço). */
    @Override
    public EstadoProcesso concluirAnalise(ProcessoPagamento processo) {
        return processo.estadoAposAnalise();
    }
}
