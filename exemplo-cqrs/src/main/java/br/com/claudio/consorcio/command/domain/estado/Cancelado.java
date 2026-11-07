package br.com.claudio.consorcio.command.domain.estado;

public record Cancelado() implements EstadoProcesso {

    public static final String NOME = "CANCELADO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public boolean finalizado() {
        return true;
    }
}
