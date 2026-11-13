package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;

/**
 * Padrão: Máquina de estados como modelo de teste [Ammann e Offutt].
 * Escrita a partir das regras 2 a 6, 10, 11 e 23 do caso de uso (fatia 1); nada aqui vem do código da aplicação.
 */
final class ModeloPagamentoProcesso {

    static final String ABERTO = "ABERTO";
    static final String EM_ANALISE = "EM_ANALISE";
    static final String AGUARDANDO_VISTORIA = "AGUARDANDO_VISTORIA";
    static final String PENDENTE_DOCUMENTO = "PENDENTE_DOCUMENTO";
    static final String REPROVADO = "REPROVADO";
    static final String APROVADO = "APROVADO";
    static final String PAGO = "PAGO";
    static final String CANCELADO = "CANCELADO";

    static final String INICIAR_ANALISE = "iniciar-analise";
    static final String SOLICITAR_DOCUMENTO = "solicitar-documento";
    static final String RECEBER_DOCUMENTO = "receber-documento";
    static final String REPROVAR = "reprovar";
    static final String DEVOLVER_PARA_ANALISE = "devolver-para-analise";
    static final String CONCLUIR_ANALISE = "concluir-analise";
    static final String CONCLUIR_VISTORIA = "concluir-vistoria";
    static final String PAGAR = "pagar";
    static final String CANCELAR = "cancelar";

    static final String SERVICO = "SERVICO";
    static final String IMOBILIARIO = "IMOBILIARIO";

    private static final List<String> ACOES = List.of(INICIAR_ANALISE, SOLICITAR_DOCUMENTO, RECEBER_DOCUMENTO,
            REPROVAR, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, DEVOLVER_PARA_ANALISE, PAGAR, CANCELAR);

    private static final List<String> SITUACOES = List.of(ABERTO, EM_ANALISE, PENDENTE_DOCUMENTO,
            AGUARDANDO_VISTORIA, APROVADO, PAGO, REPROVADO, CANCELADO);

    /** Ações cujo corpo é obrigatório e traz o motivo (R4, R10, R11, R23). */
    private static final Set<String> ACOES_COM_MOTIVO =
            Set.of(SOLICITAR_DOCUMENTO, REPROVAR, DEVOLVER_PARA_ANALISE, CANCELAR);

    /** Ações cujo corpo é opcional e pode trazer uma observação. */
    private static final Set<String> ACOES_COM_OBSERVACAO =
            Set.of(INICIAR_ANALISE, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, RECEBER_DOCUMENTO);

    private static final Map<String, Map<String, Map<String, String>>> TRANSICOES = Map.of(
            SERVICO, Map.of(
                    ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    EM_ANALISE, Map.of(SOLICITAR_DOCUMENTO, PENDENTE_DOCUMENTO, REPROVAR, REPROVADO,
                            CONCLUIR_ANALISE, APROVADO, CANCELAR, CANCELADO),
                    PENDENTE_DOCUMENTO, Map.of(RECEBER_DOCUMENTO, EM_ANALISE, CANCELAR, CANCELADO),
                    APROVADO, Map.of(PAGAR, PAGO, DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    PAGO, Map.of(),
                    REPROVADO, Map.of(),
                    CANCELADO, Map.of()),
            IMOBILIARIO, Map.of(
                    ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    EM_ANALISE, Map.of(SOLICITAR_DOCUMENTO, PENDENTE_DOCUMENTO, REPROVAR, REPROVADO,
                            CONCLUIR_ANALISE, AGUARDANDO_VISTORIA, CANCELAR, CANCELADO),
                    PENDENTE_DOCUMENTO, Map.of(RECEBER_DOCUMENTO, EM_ANALISE, CANCELAR, CANCELADO),
                    AGUARDANDO_VISTORIA, Map.of(CONCLUIR_VISTORIA, APROVADO, DEVOLVER_PARA_ANALISE, EM_ANALISE,
                            CANCELAR, CANCELADO),
                    APROVADO, Map.of(PAGAR, PAGO, DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    PAGO, Map.of(),
                    REPROVADO, Map.of(),
                    CANCELADO, Map.of()));

    /** Um passo do fluxo: a ação pedida e a situação esperada depois dela. */
    record Passo(String acao, String situacao) {
    }

    /** Caminho do início até uma situação, na ordem em que o usuário age. */
    record Caminho(String tipo, List<Passo> passos) {

        String situacaoFinal() {
            return passos.isEmpty() ? ABERTO : passos.getLast().situacao();
        }

        @Override
        public String toString() {
            return tipo + ": " + ABERTO + passos.stream().map(p -> " -> " + p.situacao()).reduce("", String::concat);
        }
    }

    /** Ação que a regra 5 manda recusar, a partir de uma situação alcançada pelo caminho. */
    record AcaoInvalida(Caminho ate, String acao) {

        @Override
        public String toString() {
            return "%s em %s (%s)".formatted(acao, ate.situacaoFinal(), ate.tipo());
        }
    }

    /** Transição do modelo: de uma situação, por uma ação, para um destino. */
    record Base(String tipo, String situacao, String acao, String destino) {

        @Override
        public String toString() {
            return "%s: %s em %s -> %s".formatted(tipo, acao, situacao, destino);
        }
    }

    private ModeloPagamentoProcesso() {
    }

    static boolean exigeMotivo(String acao) {
        return ACOES_COM_MOTIVO.contains(acao);
    }

    /** Motivo válido e identificável, usado quando o teste só precisa que a ação seja aceita. */
    static String motivoPadrao(String acao) {
        return "Motivo do teste de aceitação para " + acao;
    }

    /**
     * Valor do campo "acao" no histórico. Padrão observado no sistema atual para as ações já existentes
     * (INICIAR_ANALISE, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, PAGAR, CANCELAR): nome do caminho em maiúsculas,
     * com "_" no lugar de "-". As ações novas seguem o mesmo padrão.
     */
    static String nomeNoHistorico(String acao) {
        return acao.toUpperCase().replace('-', '_');
    }

    /** Ações aceitas na situação, pelo modelo. */
    static Set<String> acoesAceitas(String tipo, String situacao) {
        return proximas(tipo, situacao).keySet();
    }

    /** Todas as transições do modelo cuja ação é de motivo obrigatório. */
    static Stream<Base> transicoesComMotivo() {
        return transicoes(ACOES_COM_MOTIVO);
    }

    /** Todas as transições do modelo cuja ação tem observação opcional. */
    static Stream<Base> transicoesComObservacao() {
        return transicoes(ACOES_COM_OBSERVACAO);
    }

    /** Uma transição por ação, preferindo serviço, entre as ações que recebem corpo. */
    static Stream<Base> umaTransicaoPorAcaoComCorpo() {
        Set<String> vistas = new HashSet<>();
        Set<String> comCorpo = new HashSet<>(ACOES_COM_MOTIVO);
        comCorpo.addAll(ACOES_COM_OBSERVACAO);
        return transicoes(comCorpo).filter(base -> vistas.add(base.acao()));
    }

    /** Uma transição por ação de motivo obrigatório, preferindo serviço. */
    static Stream<Base> umaTransicaoPorAcaoComMotivo() {
        Set<String> vistas = new HashSet<>();
        return transicoesComMotivo().filter(base -> vistas.add(base.acao()));
    }

    private static Stream<Base> transicoes(Set<String> acoes) {
        return tipos().flatMap(tipo -> SITUACOES.stream().flatMap(situacao -> ACOES.stream()
                .filter(acoes::contains)
                .flatMap(acao -> proxima(tipo, situacao, acao)
                        .map(destino -> new Base(tipo, situacao, acao, destino)).stream())));
    }

    /** Menor caminho do início até a situação. */
    static Caminho caminhoAte(String tipo, String situacao) {
        return primeiroCaminhoAte(tipo, situacao);
    }

    /** Um menor caminho para cada situação de cada tipo. */
    static Stream<Caminho> caminhosPorSituacao() {
        return caminhosUnicos();
    }

    static Stream<String> tipos() {
        return Stream.of(SERVICO, IMOBILIARIO);
    }

    static Stream<String> acoes() {
        return ACOES.stream();
    }

    /**
     * Todos os caminhos do início até uma situação final. O grafo tem ciclos (documento, devolução), então
     * cada transição é usada no máximo uma vez por caminho: cobre todas as transições e todos os finais.
     */
    static Stream<Caminho> caminhosCompletos() {
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> proximas(tipo, caminho.situacaoFinal()).isEmpty()));
    }

    /** Cada par situação × ação que não é uma transição permitida. */
    static Stream<AcaoInvalida> acoesInvalidas() {
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.equals(primeiroCaminhoAte(tipo, caminho.situacaoFinal())))
                .flatMap(caminho -> ACOES.stream()
                        .filter(acao -> proxima(tipo, caminho.situacaoFinal(), acao).isEmpty())
                        .map(acao -> new AcaoInvalida(caminho, acao))));
    }

    /** Situações em que o processo ainda está em andamento (regra 6), com o caminho para chegar a elas. */
    static Stream<Caminho> caminhosEmAndamento() {
        return caminhosUnicos().filter(c -> !proximas(c.tipo(), c.situacaoFinal()).isEmpty());
    }

    /** Situações finais (regra 4), com o caminho para chegar a elas. */
    static Stream<Caminho> caminhosFinalizados() {
        return caminhosUnicos().filter(c -> proximas(c.tipo(), c.situacaoFinal()).isEmpty());
    }

    private static Stream<Caminho> caminhosUnicos() {
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.equals(primeiroCaminhoAte(tipo, caminho.situacaoFinal()))));
    }

    private static Caminho primeiroCaminhoAte(String tipo, String situacao) {
        return percorrer(tipo, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.situacaoFinal().equals(situacao))
                .min(java.util.Comparator.comparingInt(caminho -> caminho.passos().size()))
                .orElseThrow();
    }

    /** Busca em profundidade; devolve o caminho atual e todos os que partem dele, sem repetir transição. */
    private static List<Caminho> percorrer(String tipo, String situacao, List<Passo> passos, Set<String> usadas) {
        List<Caminho> caminhos = new ArrayList<>();
        caminhos.add(new Caminho(tipo, passos));
        for (String acao : ACOES) {
            String transicao = situacao + "/" + acao;
            if (usadas.contains(transicao)) {
                continue;
            }
            proxima(tipo, situacao, acao).ifPresent(destino -> {
                List<Passo> seguintes = new ArrayList<>(passos);
                seguintes.add(new Passo(acao, destino));
                Set<String> novas = new HashSet<>(usadas);
                novas.add(transicao);
                caminhos.addAll(percorrer(tipo, destino, List.copyOf(seguintes), Set.copyOf(novas)));
            });
        }
        return caminhos;
    }

    private static Optional<String> proxima(String tipo, String situacao, String acao) {
        return Optional.ofNullable(proximas(tipo, situacao).get(acao));
    }

    private static Map<String, String> proximas(String tipo, String situacao) {
        // Situação que o tipo não tem (ex.: aguardando vistoria no serviço) não aceita ação nenhuma.
        return TRANSICOES.get(tipo).getOrDefault(situacao, Map.of());
    }
}
