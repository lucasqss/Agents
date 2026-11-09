package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.estado.Aberto;
import br.com.claudio.consorcio.domain.processo.estado.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.estado.Pago;

/**
 * Padrões: State e Strategy, contexto [GoF]; Factory Method em novo/reconstituir [GoF].
 * Motor único dos processos de pagamento: o estado atual decide se a ação vale, o fluxo do tipo decide os desvios.
 */
public final class ProcessoPagamento {

    private final Long id;
    private final Long cotaId;
    private final TipoProcesso tipo;
    private EstadoProcesso estado;
    private String protocoloPagamento;

    private ProcessoPagamento(Long id, Long cotaId, TipoProcesso tipo, EstadoProcesso estado, String protocoloPagamento) {
        if (cotaId == null) {
            throw new DominioException("Processo precisa de uma cota");
        }
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        this.id = id;
        this.cotaId = cotaId;
        this.tipo = tipo;
        this.estado = estado == null ? new Aberto() : estado;
        if (protocoloPagamento != null) {
            registrarProtocolo(protocoloPagamento);
        }
    }

    /** Cria um processo novo, no estado Aberto, do tipo informado. */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId) {
        return new ProcessoPagamento(null, cotaId, tipo, new Aberto(), null);
    }

    /** Recria um processo já gravado, no estado em que ele estava. */
    public static ProcessoPagamento reconstituir(Long id, Long cotaId, TipoProcesso tipo, EstadoProcesso estado,
            String protocoloPagamento) {
        return new ProcessoPagamento(id, cotaId, tipo, estado, protocoloPagamento);
    }

    public void iniciarAnalise() {
        estado = estado.iniciarAnalise();
    }

    public void concluirAnalise() {
        estado = estado.concluirAnalise(tipo.fluxo());
    }

    public void concluirVistoria() {
        estado = estado.concluirVistoria();
    }

    public void pagar() {
        estado = estado.pagar();
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
        estado = estado.cancelar();
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

    public TipoProcesso tipo() {
        return tipo;
    }

    public EstadoProcesso estado() {
        return estado;
    }

    public String protocoloPagamento() {
        return protocoloPagamento;
    }
}
