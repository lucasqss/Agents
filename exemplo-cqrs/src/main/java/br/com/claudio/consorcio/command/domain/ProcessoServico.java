package br.com.claudio.consorcio.command.domain;

import br.com.claudio.consorcio.command.domain.estado.Aprovado;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;

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
