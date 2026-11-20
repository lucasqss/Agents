package br.com.claudio.pagamento.application;

/** O recurso pedido não existe. O entrypoint a traduz para 404. */
public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " não encontrado(a)");
    }
}
