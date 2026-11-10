package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.util.Set;

/**
 * Motor único dos processos de pagamento, de todos os tipos. Ele não conhece nenhum tipo em particular:
 * pergunta ao fluxo do tipo (tabela de transições) se a ação vale no estado atual e para onde ela leva.
 * Padrão: Factory Method em novo/reconstituir [GoF].
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
        if (estado == null) {
            throw new DominioException("Estado do processo é obrigatório");
        }
        this.id = id;
        this.cotaId = cotaId;
        this.tipo = tipo;
        this.estado = estado;
        if (protocoloPagamento != null) {
            registrarProtocolo(protocoloPagamento);
        }
    }

    /** Cria um processo novo, no estado Aberto, do tipo informado. */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId) {
        return new ProcessoPagamento(null, cotaId, tipo, EstadoProcesso.ABERTO, null);
    }

    /** Recria um processo já gravado, no estado em que ele estava. */
    public static ProcessoPagamento reconstituir(Long id, Long cotaId, TipoProcesso tipo, EstadoProcesso estado,
            String protocoloPagamento) {
        return new ProcessoPagamento(id, cotaId, tipo, estado, protocoloPagamento);
    }

    /** Aplica a ação; se o fluxo do tipo não a aceita no estado atual, lança TransicaoInvalidaException. */
    public void executar(AcaoProcesso acao) {
        estado = tipo.fluxo().proximo(estado, acao);
    }

    /** Guarda o protocolo emitido pela tesouraria; só faz sentido para um processo já pago. */
    public void registrarProtocolo(String protocolo) {
        if (estado != EstadoProcesso.PAGO) {
            throw new DominioException("Protocolo de pagamento só pode ser registrado em processo pago");
        }
        if (protocolo == null || protocolo.isBlank()) {
            throw new DominioException("Protocolo de pagamento é obrigatório");
        }
        this.protocoloPagamento = protocolo;
    }

    /** Ações que o fluxo do tipo aceita no estado atual. */
    public Set<AcaoProcesso> acoesDisponiveis() {
        return tipo.fluxo().acoesDisponiveis(estado);
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
