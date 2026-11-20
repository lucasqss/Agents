package br.com.claudio.pagamento.domain.processo.fluxo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Padrão: State dirigido por tabela [GoF, State, "table-driven approach"].
 * Tabela imutável (estado, ação) -> próximo estado. Há uma por tipo de processo: todas partem de {@link #comum()} e
 * cada tipo acrescenta ou troca as linhas em que difere. Uma linha pode ter dois destinos, escolhidos por uma
 * {@link CondicaoDaTransicao} declarada na própria tabela ({@code transicaoCondicional}).
 */
public final class FluxoProcesso {

    private final Map<EstadoProcesso, Map<AcaoProcesso, Destino>> transicoes;

    /** Destino de uma linha: fixo, ou função do contexto quando a linha é condicional. */
    @FunctionalInterface
    private interface Destino extends Function<ContextoDaTransicao, EstadoProcesso> {
    }

    private FluxoProcesso(Map<EstadoProcesso, Map<AcaoProcesso, Destino>> transicoes) {
        this.transicoes = transicoes;
    }

    /** Caminho que todo tipo segue, salvo as linhas que ele trocar. */
    public static Builder comum() {
        return new Builder()
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.SOLICITAR_DOCUMENTO, EstadoProcesso.PENDENTE_DOCUMENTO)
                .transicao(EstadoProcesso.PENDENTE_DOCUMENTO, AcaoProcesso.RECEBER_DOCUMENTO, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.REPROVAR, EstadoProcesso.REPROVADO)
                // R4: acima da alçada, alguém aprova na alçada; até a alçada, inclusive, aprova direto.
                .transicaoCondicional(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE,
                        CondicaoDaTransicao.VALOR_PASSA_DA_ALCADA, EstadoProcesso.AGUARDANDO_ALCADA, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.APROVAR_NA_ALCADA, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.REPROVAR, EstadoProcesso.REPROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.PAGAR, EstadoProcesso.PAGO);
    }

    /**
     * Próximo estado, ou {@link TransicaoInvalidaException} se a ação não vale no estado atual. Numa linha condicional, o
     * destino sai da condição avaliada com o contexto.
     */
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao, ContextoDaTransicao contexto) {
        Destino destino = saidas(atual).get(acao);
        if (destino == null) {
            throw new TransicaoInvalidaException(acao, atual);
        }
        return destino.apply(contexto);
    }

    /** Ações que valem no estado. */
    public Set<AcaoProcesso> acoesDisponiveis(EstadoProcesso atual) {
        return saidas(atual).keySet();
    }

    /** A tabela tem uma entrada para cada estado (o Builder cria todas). */
    private Map<AcaoProcesso, Destino> saidas(EstadoProcesso estado) {
        return Objects.requireNonNull(transicoes.get(estado));
    }

    public static final class Builder {

        private final Map<EstadoProcesso, Map<AcaoProcesso, Destino>> transicoes = new EnumMap<>(EstadoProcesso.class);

        private Builder() {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                transicoes.put(estado, new EnumMap<>(AcaoProcesso.class));
            }
        }

        /** Acrescenta a linha; se já existe uma para o mesmo estado e ação, troca a linha. */
        public Builder transicao(EstadoProcesso de, AcaoProcesso acao, EstadoProcesso para) {
            return linha(de, acao, contexto -> para);
        }

        /**
         * Linha de dois destinos: {@code seVerdadeira} quando a condição vale para o contexto, {@code seFalsa} quando não.
         * Troca a linha do mesmo estado e ação, condicional ou não.
         */
        public Builder transicaoCondicional(EstadoProcesso de, AcaoProcesso acao, CondicaoDaTransicao condicao,
                EstadoProcesso seVerdadeira, EstadoProcesso seFalsa) {
            return linha(de, acao, contexto -> condicao.vale(contexto) ? seVerdadeira : seFalsa);
        }

        /**
         * Fecha a tabela. R7, regra de todos os tipos: qualquer estado não final que o tipo usa (tem alguma saída) pode
         * ser cancelado. Estado que o tipo não usa (Aguardando vistoria, no serviço) fica sem nenhuma linha.
         */
        public FluxoProcesso construir() {
            Map<EstadoProcesso, Map<AcaoProcesso, Destino>> tabela = new EnumMap<>(EstadoProcesso.class);
            transicoes.forEach((estado, saidas) -> {
                Map<AcaoProcesso, Destino> linhas = new EnumMap<>(saidas);
                if (!estado.finalizado() && !saidas.isEmpty()) {
                    linhas.put(AcaoProcesso.CANCELAR, contexto -> EstadoProcesso.CANCELADO);
                }
                tabela.put(estado, Collections.unmodifiableMap(linhas));
            });
            return new FluxoProcesso(Collections.unmodifiableMap(tabela));
        }

        private Builder linha(EstadoProcesso de, AcaoProcesso acao, Destino destino) {
            if (de.finalizado()) {
                throw new IllegalArgumentException("Estado final não tem saída: " + de);
            }
            Objects.requireNonNull(transicoes.get(de)).put(acao, destino);
            return this;
        }
    }
}
