package br.com.claudio.consorcio.domain.processo.fluxo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

/**
 * Padrão: State dirigido por tabela [GoF, State, "table-driven approach"].
 * Tabela imutável (estado, ação) -> próximo estado. Há uma por tipo de processo: todas partem de
 * {@link #comum()} e cada tipo acrescenta ou troca as linhas em que difere.
 */
public final class FluxoProcesso {

    private final Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes;

    private FluxoProcesso(Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes) {
        this.transicoes = transicoes;
    }

    /** Caminho que todo tipo segue, salvo as linhas que ele trocar. */
    public static Builder comum() {
        return new Builder()
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.PAGAR, EstadoProcesso.PAGO);
    }

    /** Próximo estado, ou {@link TransicaoInvalidaException} se a ação não vale no estado atual. */
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao) {
        EstadoProcesso destino = transicoes.get(atual).get(acao);
        if (destino == null) {
            throw new TransicaoInvalidaException(acao, atual);
        }
        return destino;
    }

    public Set<AcaoProcesso> acoesDisponiveis(EstadoProcesso atual) {
        return transicoes.get(atual).keySet();
    }

    public static final class Builder {

        private final Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> transicoes =
                new EnumMap<>(EstadoProcesso.class);

        private Builder() {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                transicoes.put(estado, new EnumMap<>(AcaoProcesso.class));
            }
        }

        /** Acrescenta a linha; se já existe uma para o mesmo estado e ação, troca o destino. */
        public Builder transicao(EstadoProcesso de, AcaoProcesso acao, EstadoProcesso para) {
            if (de.finalizado()) {
                throw new IllegalArgumentException("Estado final não tem saída: " + de);
            }
            transicoes.get(de).put(acao, para);
            return this;
        }

        /** Fecha a tabela. Regra de todos os tipos: qualquer estado não final pode ser cancelado. */
        public FluxoProcesso construir() {
            Map<EstadoProcesso, Map<AcaoProcesso, EstadoProcesso>> tabela = new EnumMap<>(EstadoProcesso.class);
            transicoes.forEach((estado, saidas) -> {
                Map<AcaoProcesso, EstadoProcesso> linhas = new EnumMap<>(saidas);
                if (!estado.finalizado()) {
                    linhas.put(AcaoProcesso.CANCELAR, EstadoProcesso.CANCELADO);
                }
                tabela.put(estado, Collections.unmodifiableMap(linhas));
            });
            return new FluxoProcesso(Collections.unmodifiableMap(tabela));
        }
    }
}
