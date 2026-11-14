package br.com.claudio.consorcio.domain.processo.fluxo;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Padrão: State dirigido por tabela [GoF, State, "table-driven approach"].
 * Tabela imutável (estado, ação) -> próximo estado. Há uma por tipo de processo: todas partem de
 * {@link #comum()} e cada tipo acrescenta ou troca as linhas em que difere. Uma linha pode ter dois destinos, escolhidos
 * por uma {@link CondicaoDaTransicao} declarada na própria tabela ({@code transicaoCondicional}). Cada linha declara
 * também quem a pode pedir ({@link Papel}, R18) e, quando é o caso, que exige pessoa diferente da que concluiu a
 * análise (R17): assim o papel certo por situação (reprovar, devolver para análise) fica na tabela, não em
 * {@code if} espalhado. As linhas da rotina de pagamento não têm papel: nenhuma pessoa as pede.
 */
public final class FluxoProcesso {

    private final Map<EstadoProcesso, Map<AcaoProcesso, Linha>> transicoes;

    /** Destino de uma linha: fixo, ou função do contexto quando a linha é condicional. */
    @FunctionalInterface
    private interface Destino extends Function<ContextoDaTransicao, EstadoProcesso> {
    }

    /** Uma linha da tabela: para onde vai, quem a pode pedir e se exige pessoa diferente da que concluiu a análise. */
    private record Linha(Destino destino, Set<Papel> papeis, boolean exigeOutraPessoaQueConcluiuAAnalise) {

        Linha comSegundaPessoa() {
            return new Linha(destino, papeis, true);
        }
    }

    private FluxoProcesso(Map<EstadoProcesso, Map<AcaoProcesso, Linha>> transicoes) {
        this.transicoes = transicoes;
    }

    /** Caminho que todo tipo segue, salvo as linhas que ele trocar. */
    public static Builder comum() {
        return new Builder()
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.INICIAR_ANALISE, EstadoProcesso.EM_ANALISE, Papel.BACKOFFICE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.SOLICITAR_DOCUMENTO, EstadoProcesso.PENDENTE_DOCUMENTO, Papel.BACKOFFICE)
                .transicao(EstadoProcesso.PENDENTE_DOCUMENTO, AcaoProcesso.RECEBER_DOCUMENTO, EstadoProcesso.EM_ANALISE, Papel.BACKOFFICE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.REPROVAR, EstadoProcesso.REPROVADO, Papel.BACKOFFICE)
                .transicaoCondicional(EstadoProcesso.EM_ANALISE, AcaoProcesso.CONCLUIR_ANALISE,
                        CondicaoDaTransicao.VALOR_PASSA_DA_ALCADA, EstadoProcesso.AGUARDANDO_ALCADA, EstadoProcesso.APROVADO,
                        Papel.BACKOFFICE)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.APROVAR_NA_ALCADA, EstadoProcesso.APROVADO, Papel.APROVADOR)
                // R17: quem aprova na alçada não pode ser quem concluiu a análise (o imobiliário só conclui a vistoria à parte).
                .exigindoOutraPessoaQueConcluiuAAnalise(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.APROVAR_NA_ALCADA)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.REPROVAR, EstadoProcesso.REPROVADO, Papel.APROVADOR)
                .transicao(EstadoProcesso.AGUARDANDO_ALCADA, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE, Papel.APROVADOR)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.DEVOLVER_PARA_ANALISE, EstadoProcesso.EM_ANALISE, Papel.BACKOFFICE)
                .transicao(EstadoProcesso.APROVADO, AcaoProcesso.SOLICITAR_PAGAMENTO, EstadoProcesso.PAGAMENTO_SOLICITADO, Papel.TESOURARIA)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.CONFIRMAR_PAGAMENTO, EstadoProcesso.PAGO)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.RECUSAR_PAGAMENTO, EstadoProcesso.APROVADO)
                .transicao(EstadoProcesso.PAGAMENTO_SOLICITADO, AcaoProcesso.TENTAR_PAGAMENTO, EstadoProcesso.PAGAMENTO_SOLICITADO)
                .transicao(EstadoProcesso.ABERTO, AcaoProcesso.ALTERAR, EstadoProcesso.ABERTO, Papel.CLIENTE)
                .transicao(EstadoProcesso.EM_ANALISE, AcaoProcesso.PROPOR_ALTERACAO, EstadoProcesso.AGUARDANDO_CONCORDANCIA, Papel.BACKOFFICE)
                .transicao(EstadoProcesso.AGUARDANDO_CONCORDANCIA, AcaoProcesso.CONCORDAR, EstadoProcesso.EM_ANALISE, Papel.CLIENTE)
                .transicao(EstadoProcesso.AGUARDANDO_CONCORDANCIA, AcaoProcesso.DISCORDAR, EstadoProcesso.EM_ANALISE, Papel.CLIENTE)
                // Com o pagamento solicitado, só a rotina tira o processo dali: nem a pessoa cancela (R4, R20).
                .semCancelamentoEm(EstadoProcesso.PAGAMENTO_SOLICITADO);
    }

    /**
     * Próximo estado, ou {@link TransicaoInvalidaException} se a ação não vale no estado atual. Numa linha condicional, o
     * destino sai da condição avaliada com o contexto.
     */
    public EstadoProcesso proximo(EstadoProcesso atual, AcaoProcesso acao, ContextoDaTransicao contexto) {
        return linha(atual, acao).destino().apply(contexto);
    }

    /** Quem pode pedir a ação no estado (R18); vazio nas linhas da rotina. Lança {@link TransicaoInvalidaException} se a ação não vale. */
    public Set<Papel> papeisDe(EstadoProcesso atual, AcaoProcesso acao) {
        return linha(atual, acao).papeis();
    }

    /** R17: a linha exige que quem pede não seja a pessoa que concluiu a análise. Falso se a ação não vale no estado. */
    public boolean exigeOutraPessoaQueConcluiuAAnalise(EstadoProcesso atual, AcaoProcesso acao) {
        Linha linha = transicoes.get(atual).get(acao);
        return linha != null && linha.exigeOutraPessoaQueConcluiuAAnalise();
    }

    private Linha linha(EstadoProcesso atual, AcaoProcesso acao) {
        Linha linha = transicoes.get(atual).get(acao);
        if (linha == null) {
            throw new TransicaoInvalidaException(acao, atual);
        }
        return linha;
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

        private final Map<EstadoProcesso, Map<AcaoProcesso, Linha>> transicoes =
                new EnumMap<>(EstadoProcesso.class);
        private final Set<EstadoProcesso> semCancelamento = EnumSet.noneOf(EstadoProcesso.class);

        private Builder() {
            for (EstadoProcesso estado : EstadoProcesso.values()) {
                transicoes.put(estado, new EnumMap<>(AcaoProcesso.class));
            }
        }

        /**
         * Acrescenta a linha, com os papéis que a podem pedir; se já existe uma para o mesmo estado e ação, troca a linha.
         * Ação de pessoa exige ao menos um papel; ação da rotina de pagamento não tem papel.
         */
        public Builder transicao(EstadoProcesso de, AcaoProcesso acao, EstadoProcesso para, Papel... papeis) {
            return linha(de, acao, contexto -> para, papeis);
        }

        /**
         * Linha de dois destinos: {@code seVerdadeira} quando a condição vale para o contexto, {@code seFalsa} quando não.
         * Troca a linha do mesmo estado e ação, condicional ou não.
         */
        public Builder transicaoCondicional(EstadoProcesso de, AcaoProcesso acao, CondicaoDaTransicao condicao,
                EstadoProcesso seVerdadeira, EstadoProcesso seFalsa, Papel... papeis) {
            return linha(de, acao, contexto -> condicao.vale(contexto) ? seVerdadeira : seFalsa, papeis);
        }

        /** R17: na linha já declarada, quem pede não pode ser a pessoa da última conclusão da análise. */
        public Builder exigindoOutraPessoaQueConcluiuAAnalise(EstadoProcesso de, AcaoProcesso acao) {
            Linha linha = transicoes.get(de).get(acao);
            if (linha == null) {
                throw new IllegalArgumentException("Não há linha para " + acao + " em " + de);
            }
            transicoes.get(de).put(acao, linha.comSegundaPessoa());
            return this;
        }

        /** Exceção declarada à regra geral de cancelamento: neste estado a pessoa não cancela. */
        public Builder semCancelamentoEm(EstadoProcesso estado) {
            semCancelamento.add(estado);
            return this;
        }

        /**
         * Fecha a tabela. Regra de todos os tipos: qualquer estado não final pode ser cancelado, pelo backoffice ou pelo
         * aprovador (R4), salvo os declarados em {@link #semCancelamentoEm}.
         */
        public FluxoProcesso construir() {
            Map<EstadoProcesso, Map<AcaoProcesso, Linha>> tabela = new EnumMap<>(EstadoProcesso.class);
            transicoes.forEach((estado, saidas) -> {
                Map<AcaoProcesso, Linha> linhas = new EnumMap<>(saidas);
                if (!estado.finalizado() && !semCancelamento.contains(estado)) {
                    linhas.put(AcaoProcesso.CANCELAR, new Linha(contexto -> EstadoProcesso.CANCELADO,
                            EnumSet.of(Papel.BACKOFFICE, Papel.APROVADOR), false));
                }
                tabela.put(estado, Collections.unmodifiableMap(linhas));
            });
            return new FluxoProcesso(Collections.unmodifiableMap(tabela));
        }

        private Builder linha(EstadoProcesso de, AcaoProcesso acao, Destino destino, Papel[] papeis) {
            if (de.finalizado()) {
                throw new IllegalArgumentException("Estado final não tem saída: " + de);
            }
            if (acao.daRotina() != (papeis.length == 0)) {
                throw new IllegalArgumentException(acao.daRotina()
                        ? "Ação da rotina não tem papel: " + acao
                        : "Ação de pessoa precisa de ao menos um papel: " + acao);
            }
            Set<Papel> quem = papeis.length == 0 ? EnumSet.noneOf(Papel.class) : EnumSet.copyOf(List.of(papeis));
            transicoes.get(de).put(acao, new Linha(destino, Collections.unmodifiableSet(quem), false));
            return this;
        }
    }
}
