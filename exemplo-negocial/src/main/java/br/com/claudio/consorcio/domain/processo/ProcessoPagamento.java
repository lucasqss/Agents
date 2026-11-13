package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
    private final Dinheiro valor;
    private EstadoProcesso estado;
    private String protocoloPagamento;
    private final long versao;
    private final List<Transicao> novasTransicoes = new ArrayList<>();

    private ProcessoPagamento(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor, EstadoProcesso estado,
            String protocoloPagamento, long versao) {
        if (cotaId == null) {
            throw new DominioException("Processo precisa de uma cota");
        }
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        if (estado == null) {
            throw new DominioException("Estado do processo é obrigatório");
        }
        if (valor == null || !valor.positivo()) {
            throw new DominioException("Valor do processo precisa ser maior que zero");
        }
        this.id = id;
        this.cotaId = cotaId;
        this.tipo = tipo;
        this.valor = valor;
        if ((estado == EstadoProcesso.PAGO) != (protocoloPagamento != null)) {
            throw new DominioException("Processo pago tem protocolo de pagamento, e só ele");
        }
        this.estado = estado;
        this.protocoloPagamento = protocoloPagamento;
        this.versao = versao;
    }

    /** Cria um processo novo, no estado Aberto, do tipo e do valor informados (R12: maior que zero, até duas casas). */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId, Dinheiro valor) {
        return new ProcessoPagamento(null, cotaId, tipo, valor, EstadoProcesso.ABERTO, null, 0);
    }

    /** Recria um processo já gravado, no estado em que ele estava. */
    public static ProcessoPagamento reconstituir(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor,
            EstadoProcesso estado, String protocoloPagamento, long versao) {
        return new ProcessoPagamento(id, cotaId, tipo, valor, estado, protocoloPagamento, versao);
    }

    /**
     * Aplica a ação; se o fluxo do tipo não a aceita no estado atual, lança TransicaoInvalidaException.
     * Ação que exige motivo ({@link AcaoProcesso#exigeMotivo()}) é recusada sem ele; o motivo vai ao histórico
     * como observação.
     */
    public void executar(AcaoProcesso acao, Instant quando, String observacao) {
        if (tipo.fluxo().proximo(estado, acao) == EstadoProcesso.PAGO) {
            throw new DominioException("Processo só fica pago com o protocolo da tesouraria");
        }
        if (acao.exigeMotivo() && (observacao == null || observacao.isBlank())) {
            throw new DominioException("Motivo é obrigatório para " + acao.descricao() + " um processo");
        }
        transitar(acao, quando, observacao);
    }

    /** Paga o processo e guarda o protocolo emitido pela tesouraria, numa operação só. */
    public void pagar(String protocolo, Instant quando) {
        if (protocolo == null || protocolo.isBlank()) {
            throw new DominioException("Protocolo de pagamento é obrigatório");
        }
        transitar(AcaoProcesso.PAGAR, quando, null);
        protocoloPagamento = protocolo;
    }

    private void transitar(AcaoProcesso acao, Instant quando, String observacao) {
        Transicao transicao = new Transicao(estado, tipo.fluxo().proximo(estado, acao), acao, quando, observacao);
        estado = transicao.para();
        novasTransicoes.add(transicao);
    }

    /** Confere se a ação vale no estado atual, sem aplicá-la; para validar antes de um efeito externo. */
    public void exigirQuePode(AcaoProcesso acao) {
        tipo.fluxo().proximo(estado, acao);
    }

    /** Ações que o fluxo do tipo aceita no estado atual. */
    public Set<AcaoProcesso> acoesDisponiveis() {
        return tipo.fluxo().acoesDisponiveis(estado);
    }

    /** Transições feitas desde que o processo foi criado ou lido; o repositório as grava no histórico ao salvar. */
    public List<Transicao> novasTransicoes() {
        return List.copyOf(novasTransicoes);
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

    public Dinheiro valor() {
        return valor;
    }

    public EstadoProcesso estado() {
        return estado;
    }

    public String protocoloPagamento() {
        return protocoloPagamento;
    }

    /** Versão gravada quando o processo foi lido; a gravação falha se outra operação já a mudou. */
    public long versao() {
        return versao;
    }
}
