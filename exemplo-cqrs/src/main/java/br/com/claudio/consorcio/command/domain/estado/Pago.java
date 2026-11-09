package br.com.claudio.consorcio.command.domain.estado;

/**
 * Padrão: State, estado concreto [GoF].
 * Estado final: não aceita nenhuma transição.
 */
public record Pago() implements EstadoProcesso {

    public static final String NOME = "PAGO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public boolean finalizado() {
        return true;
    }
}
