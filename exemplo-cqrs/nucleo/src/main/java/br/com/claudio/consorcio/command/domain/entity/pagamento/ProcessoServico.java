package br.com.claudio.consorcio.command.domain.entity.pagamento;

import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Aprovado;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.EstadoProcesso;
import br.com.claudio.consorcio.command.domain.enums.TipoProcesso;

/** Processo de serviço: da análise vai direto para aprovado. */
public class ProcessoServico extends ProcessoPagamento {

    public ProcessoServico(Long id, Long cotaId, EstadoProcesso estado, String protocoloPagamento) {
        super(id, cotaId, estado, protocoloPagamento);
    }

    @Override
    public TipoProcesso tipo() {
        return TipoProcesso.SERVICO;
    }

    @Override
    public EstadoProcesso estadoAposAnalise() {
        return new Aprovado();
    }
}
