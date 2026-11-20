package br.com.claudio.pagamento.domain.processo;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.DominioException;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Motor único dos processos de pagamento, de todos os tipos. Ele não conhece nenhum tipo em particular: pergunta ao
 * fluxo do tipo (tabela de transições) se a ação vale no estado atual e para onde ela leva.
 * Padrão: Factory Method em novo/reconstituir [GoF].
 */
public final class ProcessoPagamento {

    public static final int TAMANHO_MAXIMO_DA_COTA = 30;

    private final @Nullable Long id;
    private final TipoProcesso tipo;
    private final String numeroCota;
    private final Dinheiro valor;
    private EstadoProcesso estado;
    private final long versao;
    private final List<Transicao> novasTransicoes = new ArrayList<>();

    private ProcessoPagamento(@Nullable Long id, TipoProcesso tipo, String numeroCota, Dinheiro valor,
            EstadoProcesso estado, long versao) {
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        if (numeroCota == null || numeroCota.isBlank()) {
            throw new DominioException("Número da cota é obrigatório");
        }
        if (numeroCota.strip().length() > TAMANHO_MAXIMO_DA_COTA) {
            throw new DominioException("Número da cota tem no máximo " + TAMANHO_MAXIMO_DA_COTA + " caracteres");
        }
        if (valor == null || !valor.positivo()) {
            throw new DominioException("Valor do processo precisa ser maior que zero");
        }
        if (estado == null) {
            throw new DominioException("Estado do processo é obrigatório");
        }
        this.id = id;
        this.tipo = tipo;
        this.numeroCota = numeroCota.strip();
        this.valor = valor;
        this.estado = estado;
        this.versao = versao;
    }

    /** R1: cria um processo novo, em Aberto, com cota informada e valor maior que zero. */
    public static ProcessoPagamento novo(TipoProcesso tipo, String numeroCota, Dinheiro valor) {
        return new ProcessoPagamento(null, tipo, numeroCota, valor, EstadoProcesso.ABERTO, 0);
    }

    /** Recria um processo já gravado, no estado em que ele estava. */
    public static ProcessoPagamento reconstituir(Long id, TipoProcesso tipo, String numeroCota, Dinheiro valor,
            EstadoProcesso estado, long versao) {
        return new ProcessoPagamento(id, tipo, numeroCota, valor, estado, versao);
    }

    /**
     * Aplica a ação e registra a transição no histórico (R8).
     * <ul>
     * <li>R2, R6: se o fluxo do tipo não a aceita no estado atual, lança {@code TransicaoInvalidaException} e nada muda.</li>
     * <li>R5: ação que exige motivo é recusada sem ele; o motivo vai ao histórico como observação.</li>
     * <li>R4: a alçada é entregue por quem chama (o domínio não lê configuração). Nas linhas condicionais, a tabela decide
     * o destino com o valor do processo e a alçada.</li>
     * </ul>
     */
    public void executar(AcaoProcesso acao, String autor, Instant quando, @Nullable String motivo,
            @Nullable Dinheiro alcada) {
        if (acao == null) {
            throw new DominioException("Ação é obrigatória");
        }
        EstadoProcesso proximo = tipo.fluxo().proximo(estado, acao, new ContextoDaTransicao(valor, alcada));
        if (acao.exigeMotivo() && (motivo == null || motivo.isBlank())) {
            throw new DominioException("Motivo é obrigatório para " + acao.descricao() + " um processo");
        }
        Transicao transicao = new Transicao(estado, proximo, acao, quando, autor, motivo);
        estado = proximo;
        novasTransicoes.add(transicao);
    }

    /** Ações que o fluxo do tipo aceita no estado atual: o front não repete a tabela para decidir os botões. */
    public Set<AcaoProcesso> acoesDisponiveis() {
        return tipo.fluxo().acoesDisponiveis(estado);
    }

    /** Transições feitas desde que o processo foi criado ou lido; o repositório as grava no histórico ao salvar. */
    public List<Transicao> novasTransicoes() {
        return List.copyOf(novasTransicoes);
    }

    public @Nullable Long id() {
        return id;
    }

    public TipoProcesso tipo() {
        return tipo;
    }

    public String numeroCota() {
        return numeroCota;
    }

    public Dinheiro valor() {
        return valor;
    }

    public EstadoProcesso estado() {
        return estado;
    }

    /** Versão gravada quando o processo foi lido; a gravação falha se outra operação já a mudou. */
    public long versao() {
        return versao;
    }
}
