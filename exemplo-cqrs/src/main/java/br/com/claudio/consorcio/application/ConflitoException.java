package br.com.claudio.consorcio.application;

/** Operação que conflita com o estado atual dos dados (duplicidade, dependências). */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
