package br.com.claudio.consorcio.domain.processo.estado;

/**
 * Padrão: State, estado concreto [GoF].
 * Estado final: não aceita nenhuma transição.
 */
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
