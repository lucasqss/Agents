package br.com.claudio.consorcio.shared.integracao;

/** Um sistema externo não respondeu a tempo, recusou a chamada ou está com o circuito aberto. */
public class IntegracaoIndisponivelException extends RuntimeException {

    private final String sistema;

    public IntegracaoIndisponivelException(String sistema, Throwable causa) {
        super(sistema + " indisponível no momento", causa);
        this.sistema = sistema;
    }

    public String sistema() {
        return sistema;
    }
}
