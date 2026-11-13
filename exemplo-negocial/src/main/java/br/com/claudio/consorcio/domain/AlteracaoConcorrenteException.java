package br.com.claudio.consorcio.domain;

/** O registro foi alterado por outra operação entre a leitura e a gravação; quem chamou deve reler e tentar de novo. */
public class AlteracaoConcorrenteException extends RuntimeException {

    public AlteracaoConcorrenteException(String recurso, Long id) {
        super(recurso + " " + id + " foi alterado por outra operação; consulte de novo e repita");
    }
}
