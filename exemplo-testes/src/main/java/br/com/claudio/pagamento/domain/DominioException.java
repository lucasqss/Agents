package br.com.claudio.pagamento.domain;

/** Violação de uma regra de negócio. O entrypoint a traduz para 400. */
public class DominioException extends RuntimeException {

    public DominioException(String mensagem) {
        super(mensagem);
    }
}
