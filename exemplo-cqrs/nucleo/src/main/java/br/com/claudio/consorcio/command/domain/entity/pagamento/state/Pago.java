package br.com.claudio.consorcio.command.domain.entity.pagamento.state;

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
