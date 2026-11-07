package br.com.claudio.consorcio.command.domain.exception;

/** Violação de uma regra de validação do domínio. */
public class DominioException extends RuntimeException {

    public DominioException(String mensagem) {
        super(mensagem);
    }
}
