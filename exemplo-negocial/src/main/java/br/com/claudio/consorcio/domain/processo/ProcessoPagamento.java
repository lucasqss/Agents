package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
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
    private Dinheiro valor;
    private List<Favorecido> favorecidos;
    private EstadoProcesso estado;
    private String protocoloPagamento;
    private AlteracaoProposta proposta;
    private final long versao;
    private final List<Transicao> novasTransicoes = new ArrayList<>();

    private ProcessoPagamento(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor,
            List<Favorecido> favorecidos, EstadoProcesso estado, String protocoloPagamento,
            AlteracaoProposta proposta, long versao) {
        if (cotaId == null) {
            throw new DominioException("Processo precisa de uma cota");
        }
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        if (estado == null) {
            throw new DominioException("Estado do processo é obrigatório");
        }
        exigirDivisaoValida(valor, favorecidos);
        this.id = id;
        this.cotaId = cotaId;
        this.tipo = tipo;
        this.valor = valor;
        this.favorecidos = List.copyOf(favorecidos);
        if ((estado == EstadoProcesso.PAGO) != (protocoloPagamento != null)) {
            throw new DominioException("Processo pago tem protocolo de pagamento, e só ele");
        }
        if ((estado == EstadoProcesso.AGUARDANDO_CONCORDANCIA) != (proposta != null)) {
            throw new DominioException("Processo tem alteração proposta se, e somente se, aguarda a concordância");
        }
        this.estado = estado;
        this.protocoloPagamento = protocoloPagamento;
        this.proposta = proposta;
        this.versao = versao;
    }

    /**
     * Cria um processo novo, no estado Aberto, do tipo e do valor informados (R12: maior que zero, até duas casas),
     * dividido entre os favorecidos (R15, R29): ao menos um, partes maiores que zero somando o valor, sem documento repetido.
     */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId, Dinheiro valor, List<Favorecido> favorecidos) {
        return new ProcessoPagamento(null, cotaId, tipo, valor, favorecidos, EstadoProcesso.ABERTO, null, null, 0);
    }

    /** Recria um processo já gravado, no estado em que ele estava; a alteração proposta existe só em Aguardando concordância. */
    public static ProcessoPagamento reconstituir(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor,
            List<Favorecido> favorecidos, EstadoProcesso estado, String protocoloPagamento,
            AlteracaoProposta proposta, long versao) {
        return new ProcessoPagamento(id, cotaId, tipo, valor, favorecidos, estado, protocoloPagamento, proposta, versao);
    }

    /**
     * Aplica a ação; se o fluxo do tipo não a aceita no estado atual, lança TransicaoInvalidaException.
     * Ação que exige motivo ({@link AcaoProcesso#exigeMotivo()}) é recusada sem ele; o motivo vai ao histórico
     * como observação. Ação com método próprio ({@link AcaoProcesso#comMetodoProprio()}) é recusada aqui.
     * Concordar torna vigente a alteração proposta; discordar e cancelar a descartam.
     */
    public void executar(AcaoProcesso acao, Instant quando, String observacao) {
        EstadoProcesso destino = tipo.fluxo().proximo(estado, acao);
        if (destino == EstadoProcesso.PAGO) {
            throw new DominioException("Processo só fica pago com o protocolo da tesouraria");
        }
        if (acao.comMetodoProprio()) {
            throw new DominioException("Para " + acao.descricao() + " um processo é preciso informar os dados da ação");
        }
        if (acao.exigeMotivo() && (observacao == null || observacao.isBlank())) {
            throw new DominioException("Motivo é obrigatório para " + acao.descricao() + " um processo");
        }
        transitar(acao, quando, observacao, null, null);
        if (acao == AcaoProcesso.CONCORDAR) {
            valor = proposta.valor();
            favorecidos = proposta.favorecidos();
        }
        if (estado != EstadoProcesso.AGUARDANDO_CONCORDANCIA) {
            proposta = null;
        }
    }

    /**
     * Troca o valor e os favorecidos vigentes (R24, só em Aberto), com as mesmas regras da abertura (R12, R15, R29).
     * Recusa o dado inválido antes de olhar o estado, e nada muda quando recusa.
     */
    public void alterar(Dinheiro novoValor, List<Favorecido> novosFavorecidos, Instant quando) {
        exigirDivisaoValida(novoValor, novosFavorecidos);
        transitar(AcaoProcesso.ALTERAR, quando, null, valor, novoValor);
        valor = novoValor;
        favorecidos = List.copyOf(novosFavorecidos);
    }

    /**
     * Guarda a alteração pedida pelo backoffice (R25, só em Análise) e passa a aguardar o cliente. O valor e os
     * favorecidos vigentes continuam os mesmos até {@code CONCORDAR}. O motivo é obrigatório e vai ao histórico.
     */
    public void proporAlteracao(Dinheiro novoValor, List<Favorecido> novosFavorecidos, String motivo, Instant quando) {
        AlteracaoProposta nova = new AlteracaoProposta(novoValor, novosFavorecidos, motivo);
        transitar(AcaoProcesso.PROPOR_ALTERACAO, quando, nova.motivo(), valor, novoValor);
        proposta = nova;
    }

    /** Paga o processo e guarda o protocolo emitido pela tesouraria, numa operação só. */
    public void pagar(String protocolo, Instant quando) {
        if (protocolo == null || protocolo.isBlank()) {
            throw new DominioException("Protocolo de pagamento é obrigatório");
        }
        transitar(AcaoProcesso.PAGAR, quando, null, null, null);
        protocoloPagamento = protocolo;
    }

    /** Valor do processo e favorecidos: valor maior que zero e divisão exata entre os favorecidos (R12, R15, R29). */
    static void exigirDivisaoValida(Dinheiro valor, List<Favorecido> favorecidos) {
        if (valor == null || !valor.positivo()) {
            throw new DominioException("Valor do processo precisa ser maior que zero");
        }
        if (favorecidos == null || favorecidos.isEmpty()) {
            throw new DominioException("Processo precisa de ao menos um favorecido");
        }
        Set<Documento> documentos = new HashSet<>();
        Dinheiro soma = Dinheiro.ZERO;
        for (Favorecido favorecido : favorecidos) {
            if (favorecido == null) {
                throw new DominioException("Favorecido não pode ser nulo");
            }
            if (!documentos.add(favorecido.documento())) {
                throw new DominioException("Favorecido repetido: o documento " + favorecido.documento().numero()
                        + " aparece mais de uma vez");
            }
            soma = soma.somar(favorecido.parte());
        }
        if (soma.compareTo(valor) < 0) {
            throw new DominioException("As partes dos favorecidos somam " + soma.valor().toPlainString() + " e faltam "
                    + valor.subtrair(soma).valor().toPlainString() + " para o valor do processo ("
                    + valor.valor().toPlainString() + ")");
        }
        if (soma.compareTo(valor) > 0) {
            throw new DominioException("As partes dos favorecidos somam " + soma.valor().toPlainString() + " e passam "
                    + soma.subtrair(valor).valor().toPlainString() + " do valor do processo ("
                    + valor.valor().toPlainString() + ")");
        }
    }

    private void transitar(AcaoProcesso acao, Instant quando, String observacao, Dinheiro valorAnterior,
            Dinheiro valorNovo) {
        Transicao transicao = new Transicao(estado, tipo.fluxo().proximo(estado, acao), acao, quando, observacao,
                valorAnterior, valorNovo);
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

    /** Alteração proposta aguardando a resposta do cliente; só existe em Aguardando concordância. */
    public Optional<AlteracaoProposta> propostaPendente() {
        return Optional.ofNullable(proposta);
    }

    /** Favorecidos vigentes, na ordem em que foram informados; só leitura. */
    public List<Favorecido> favorecidos() {
        return favorecidos;
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
