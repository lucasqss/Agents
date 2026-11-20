package br.com.claudio.pagamento.domain.processo.fluxo;

import br.com.claudio.pagamento.domain.DominioException;

/** R2, R6: ação que o fluxo do tipo não aceita no estado atual do processo. O entrypoint a traduz para 409. */
public class TransicaoInvalidaException extends DominioException {

    public TransicaoInvalidaException(AcaoProcesso acao, EstadoProcesso estadoAtual) {
        super("Não é possível " + acao.descricao() + " um processo no estado " + estadoAtual);
    }
}
