package br.com.claudio.consorcio.domain.processo.fluxo;

import br.com.claudio.consorcio.domain.DominioException;

/** Ação que o fluxo do tipo não aceita no estado atual do processo. */
public class TransicaoInvalidaException extends DominioException {

    public TransicaoInvalidaException(AcaoProcesso acao, EstadoProcesso estadoAtual) {
        super("Não é possível " + acao.descricao() + " um processo no estado " + estadoAtual);
    }
}
