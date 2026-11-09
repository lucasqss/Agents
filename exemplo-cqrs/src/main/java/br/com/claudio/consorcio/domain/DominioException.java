package br.com.claudio.consorcio.domain;

/** Violação de uma regra de validação do domínio. */
public class DominioException extends RuntimeException {

    public DominioException(String mensagem) {
        super(mensagem);
    }
}
