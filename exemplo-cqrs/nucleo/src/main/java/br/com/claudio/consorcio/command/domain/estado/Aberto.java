package br.com.claudio.consorcio.command.domain.estado;

import br.com.claudio.consorcio.command.domain.ProcessoPagamento;

public record Aberto() implements EstadoProcesso {

    public static final String NOME = "ABERTO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso iniciarAnalise(ProcessoPagamento processo) {
        return new EmAnalise();
    }
}
