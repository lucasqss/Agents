package br.com.claudio.consorcio.application;

public class NaoEncontradoException extends RuntimeException {

    public NaoEncontradoException(String recurso, Long id) {
        super(recurso + " " + id + " não encontrado(a)");
    }
}
