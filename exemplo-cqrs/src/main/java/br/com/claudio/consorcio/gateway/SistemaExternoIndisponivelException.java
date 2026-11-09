package br.com.claudio.consorcio.gateway;

/** Um sistema externo não respondeu a tempo, recusou a chamada ou está com o circuito aberto. */
public class SistemaExternoIndisponivelException extends RuntimeException {

    private final String sistema;

    public SistemaExternoIndisponivelException(String sistema, Throwable causa) {
        super(sistema + " indisponível no momento", causa);
        this.sistema = sistema;
    }

    public String sistema() {
        return sistema;
    }
}
