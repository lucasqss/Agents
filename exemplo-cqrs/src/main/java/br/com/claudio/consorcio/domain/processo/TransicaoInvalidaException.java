package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;

/** Tentativa de mudar o processo para um estado que não é permitido a partir do estado atual. */
public class TransicaoInvalidaException extends DominioException {

    public TransicaoInvalidaException(String acao, String estadoAtual) {
        super("Não é possível " + acao + " um processo no estado " + estadoAtual);
    }
}
