package br.com.claudio.consorcio.command.domain.entity.pagamento;

import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Aberto;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.EstadoProcesso;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Pago;
import br.com.claudio.consorcio.command.domain.enums.TipoProcesso;
import br.com.claudio.consorcio.command.domain.exception.DominioException;

/**
 * Processo de pagamento de uma cota. As transições são delegadas ao estado atual (padrão State);
 * cada subclasse decide para onde o processo vai depois da análise.
 */
public abstract class ProcessoPagamento {

    private final Long id;
    private final Long cotaId;
    private EstadoProcesso estado;
    private String protocoloPagamento;

    protected ProcessoPagamento(Long id, Long cotaId, EstadoProcesso estado, String protocoloPagamento) {
        if (cotaId == null) {
            throw new DominioException("Processo precisa de uma cota");
        }
        this.id = id;
        this.cotaId = cotaId;
        this.estado = estado == null ? new Aberto() : estado;
        if (protocoloPagamento != null) {
            registrarProtocolo(protocoloPagamento);
        }
    }

    /** Cria um processo novo, no estado Aberto, do tipo informado. */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId) {
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        return switch (tipo) {
            case IMOBILIARIO -> new ProcessoImobiliario(null, cotaId, new Aberto(), null);
            case SERVICO -> new ProcessoServico(null, cotaId, new Aberto(), null);
        };
    }

    public abstract TipoProcesso tipo();

    /** Próximo estado quando a análise é concluída. */
    public abstract EstadoProcesso estadoAposAnalise();

    public void iniciarAnalise() {
        estado = estado.iniciarAnalise(this);
    }

    public void concluirAnalise() {
        estado = estado.concluirAnalise(this);
    }

    public void concluirVistoria() {
        estado = estado.concluirVistoria(this);
    }

    public void pagar() {
        estado = estado.pagar(this);
    }

    /** Guarda o protocolo emitido pela tesouraria; só faz sentido para um processo já pago. */
    public void registrarProtocolo(String protocolo) {
        if (!(estado instanceof Pago)) {
            throw new DominioException("Protocolo de pagamento só pode ser registrado em processo pago");
        }
        if (protocolo == null || protocolo.isBlank()) {
            throw new DominioException("Protocolo de pagamento é obrigatório");
        }
        this.protocoloPagamento = protocolo;
    }

    public void cancelar() {
        estado = estado.cancelar(this);
    }

    public boolean ativo() {
        return !estado.finalizado();
    }

    public Long id() {
        return id;
    }

    public Long cotaId() {
        return cotaId;
    }

    public EstadoProcesso estado() {
        return estado;
    }

    public String protocoloPagamento() {
        return protocoloPagamento;
    }
}
