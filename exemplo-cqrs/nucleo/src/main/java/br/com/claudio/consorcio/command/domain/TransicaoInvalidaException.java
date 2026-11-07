package br.com.claudio.consorcio.command.domain;

/** Tentativa de mudar o processo para um estado que não é permitido a partir do estado atual. */
public class TransicaoInvalidaException extends DominioException {

    public TransicaoInvalidaException(String acao, String estadoAtual) {
        super("Não é possível " + acao + " um processo no estado " + estadoAtual);
    }
}
