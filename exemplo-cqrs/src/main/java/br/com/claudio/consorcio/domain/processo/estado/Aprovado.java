package br.com.claudio.consorcio.domain.processo.estado;

/**
 * Padrão: State, estado concreto [GoF].
 * Aprovado e pronto para pagar.
 */
public record Aprovado() implements EstadoProcesso {

    public static final String NOME = "APROVADO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso pagar() {
        return new Pago();
    }
}
