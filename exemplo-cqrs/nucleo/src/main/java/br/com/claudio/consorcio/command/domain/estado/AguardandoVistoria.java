package br.com.claudio.consorcio.command.domain.estado;

import br.com.claudio.consorcio.command.domain.ProcessoPagamento;

public record AguardandoVistoria() implements EstadoProcesso {

    public static final String NOME = "AGUARDANDO_VISTORIA";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso concluirVistoria(ProcessoPagamento processo) {
        return new Aprovado();
    }
}
