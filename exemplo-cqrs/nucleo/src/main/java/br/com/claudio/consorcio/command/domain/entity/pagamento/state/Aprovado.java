package br.com.claudio.consorcio.command.domain.entity.pagamento.state;

import br.com.claudio.consorcio.command.domain.entity.pagamento.ProcessoPagamento;

public record Aprovado() implements EstadoProcesso {

    public static final String NOME = "APROVADO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso pagar(ProcessoPagamento processo) {
        return new Pago();
    }
}
