package br.com.claudio.consorcio.command.domain;

import br.com.claudio.consorcio.command.domain.estado.AguardandoVistoria;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;

/** Processo imobiliário: depois da análise, aguarda vistoria antes de ser aprovado. */
public class ProcessoImobiliario extends ProcessoPagamento {

    public ProcessoImobiliario(Long id, Long cotaId, EstadoProcesso estado, String protocoloPagamento) {
        super(id, cotaId, estado, protocoloPagamento);
    }

    @Override
    public TipoProcesso tipo() {
        return TipoProcesso.IMOBILIARIO;
    }

    @Override
    public EstadoProcesso estadoAposAnalise() {
        return new AguardandoVistoria();
    }
}
