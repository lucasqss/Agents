package br.com.claudio.consorcio.domain.processo.fluxo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Padrão: State dirigido por tabela [GoF, State, "table-driven approach"].
 * Tabela imutável (estado, ação) -> próximo estado. Há uma por tipo de processo: todas partem de
 * {@link #comum()} e cada tipo acrescenta ou troca as linhas em que difere. Uma linha pode ter dois destinos, escolhidos
 * por uma {@link CondicaoDaTransicao} declarada na própria tabela ({@code transicaoCondicional}).
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
                .transicaoCondicional(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE,
                        CondicaoDaTransicao.VALOR_PASSA_DA_ALCADA, EstadoProcesso.AGUARDANDO_ALCADA, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.APROVAR_NA_ALCADA, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.REPROVAR, EstadoProcesso.REPROVADO)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.SOLICITAR_PAGAMENTO, EstadoProcesso.PAGAMENTO_SOLICITADO)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.CONFIRMAR_PAGAMENTO, EstadoProcesso.PAGO)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.RECUSAR_PAGAMENTO, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.TENTAR_PAGAMENTO, EstadoProcesso.PAGAMENTO_SOLICITADO)
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.ALTERAR, EstadoProcesso.ABERTO)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.PROPOR_ALTERACAO, EstadoProcesso.AGUARDANDO_CONCORDANCIA)
                .transicao(EstadoProcesso.AGUARDANDO_CONCORDANCIA, AcaoProcesso.CONCORDAR, EstadoProcesso.EM_ANALISE)
                .transicao(EstadoProcesso.AGUARDANDO_CONCORDANCIA, AcaoProcesso.DISCORDAR, EstadoProcesso.EM_ANALISE)
                // Com o pagamento solicitado, só a rotina tira o processo dali: nem a pessoa cancela (R4, R20).
                .semCancelamentoEm(EstadoProcesso.PAGAMENTO_SOLICITADO);
    }

    /**
     * Próximo estado, ou {@link TransicaoInvalidaException} se a ação não vale no estado atual. Numa linha condicional, o
     * destino sai da condição avaliada com o contexto.
     */
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao, ContextoDaTransicao contexto) {
        Destino destino = transicoes.get(atual).get(acao);
        if (destino == null) {
            throw new TransicaoInvalidaException(acao, atual);
        }
        return destino.apply(contexto);
    }

    /** Ações que uma pessoa pode pedir no estado: as da rotina de pagamento valem na tabela, mas não aparecem aqui. */
    public Set<AcaoProcesso> acoesDisponiveis(EstadoProcesso atual) {
        Set<AcaoProcesso> daPessoa = EnumSet.noneOf(AcaoProcesso.class);
        for (AcaoProcesso acao : transicoes.get(atual).keySet()) {
            if (!acao.daRotina()) {
                daPessoa.add(acao);
            }
        }
        return Collections.unmodifiableSet(daPessoa);
    }

    public static final class Builder {

        private final Map<EstadoProcesso, Map<AcaoProcesso, Destino>> transicoes =
                new EnumMap<>(EstadoProcesso.class);
        private final Set<EstadoProcesso> semCancelamento = EnumSet.noneOf(EstadoProcesso.class);

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
            transicoes.get(de).put(acao, contexto -> para);
            return this;
        }

        /**
         * Linha de dois destinos: {@code seVerdadeira} quando a condição vale para o contexto, {@code seFalsa} quando não.
         * Troca a linha do mesmo estado e ação, condicional ou não.
         */
        public Builder transicaoCondicional(EstadoProcesso de, AcaoProcesso acao, CondicaoDaTransicao condicao,
                EstadoProcesso seVerdadeira, EstadoProcesso seFalsa) {
            if (de.finalizado()) {
                throw new IllegalArgumentException("Estado final não tem saída: " + de);
            }
            transicoes.get(de).put(acao, contexto -> condicao.vale(contexto) ? seVerdadeira : seFalsa);
            return this;
        }

        /** Exceção declarada à regra geral de cancelamento: neste estado a pessoa não cancela. */
        public Builder semCancelamentoEm(EstadoProcesso estado) {
            semCancelamento.add(estado);
            return this;
        }

        /**
         * Fecha a tabela. Regra de todos os tipos: qualquer estado não final pode ser cancelado, salvo os declarados
         * em {@link #semCancelamentoEm}.
         */
        public FluxoProcesso construir() {
            Map<EstadoProcesso, Map<AcaoProcesso, Destino>> tabela = new EnumMap<>(EstadoProcesso.class);
            transicoes.forEach((estado, saidas) -> {
                Map<AcaoProcesso, Destino> linhas = new EnumMap<>(saidas);
                if (!estado.finalizado() && !semCancelamento.contains(estado)) {
                    linhas.put(AcaoProcesso.CANCELAR, contexto -> EstadoProcesso.CANCELADO);
                }
                tabela.put(estado, Collections.unmodifiableMap(linhas));
            });
            return new FluxoProcesso(Collections.unmodifiableMap(tabela));
        }
    }
}
