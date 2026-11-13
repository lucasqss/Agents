package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Padrão: Máquina de estados como modelo de teste [Ammann e Offutt].
 * Escrita a partir das regras 2 a 5 do caso de uso; nada aqui vem do código da aplicação.
 */
final class ModeloPagamentoProcesso {

    static final String ABERTO = "ABERTO";
    static final String EM_ANALISE = "EM_ANALISE";
    static final String AGUARDANDO_VISTORIA = "AGUARDANDO_VISTORIA";
    static final String APROVADO = "APROVADO";
    static final String PAGO = "PAGO";
    static final String CANCELADO = "CANCELADO";

    static final String INICIAR_ANALISE = "iniciar-analise";
    static final String CONCLUIR_ANALISE = "concluir-analise";
    static final String CONCLUIR_VISTORIA = "concluir-vistoria";
    static final String PAGAR = "pagar";
    static final String CANCELAR = "cancelar";

    static final String SERVICO = "SERVICO";
    static final String IMOBILIARIO = "IMOBILIARIO";

    private static final List<String> ACOES =
            List.of(INICIAR_ANALISE, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, PAGAR, CANCELAR);

    private static final Map<String, Map<String, Map<String, String>>> TRANSICOES = Map.of(
            SERVICO, Map.of(
                    ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    EM_ANALISE, Map.of(CONCLUIR_ANALISE, APROVADO, CANCELAR, CANCELADO),
                    APROVADO, Map.of(PAGAR, PAGO, CANCELAR, CANCELADO),
                    PAGO, Map.of(),
                    CANCELADO, Map.of()),
            IMOBILIARIO, Map.of(
                    ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO),
                    EM_ANALISE, Map.of(CONCLUIR_ANALISE, AGUARDANDO_VISTORIA, CANCELAR, CANCELADO),
                    AGUARDANDO_VISTORIA, Map.of(CONCLUIR_VISTORIA, APROVADO, CANCELAR, CANCELADO),
                    APROVADO, Map.of(PAGAR, PAGO, CANCELAR, CANCELADO),
                    PAGO, Map.of(),
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

    private ModeloPagamentoProcesso() {
    }

    static Stream<String> tipos() {
        return Stream.of(SERVICO, IMOBILIARIO);
    }

    static Stream<String> acoes() {
        return ACOES.stream();
    }

    /** Todos os caminhos do início até uma situação final (o grafo não tem ciclos, então são poucos). */
    static Stream<Caminho> caminhosCompletos() {
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of()).stream()
                .filter(caminho -> proximas(tipo, caminho.situacaoFinal()).isEmpty()));
    }

    /** Cada par situação × ação que não é uma transição permitida. */
    static Stream<AcaoInvalida> acoesInvalidas() {
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of()).stream()
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
        return tipos().flatMap(tipo -> percorrer(tipo, ABERTO, List.of()).stream()
                .filter(caminho -> caminho.equals(primeiroCaminhoAte(tipo, caminho.situacaoFinal()))));
    }

    private static Caminho primeiroCaminhoAte(String tipo, String situacao) {
        return percorrer(tipo, ABERTO, List.of()).stream()
                .filter(caminho -> caminho.situacaoFinal().equals(situacao))
                .findFirst()
                .orElseThrow();
    }

    /** Busca em profundidade; devolve o caminho atual e todos os que partem dele, na ordem fixa das ações. */
    private static List<Caminho> percorrer(String tipo, String situacao, List<Passo> passos) {
        List<Caminho> caminhos = new ArrayList<>();
        caminhos.add(new Caminho(tipo, passos));
        for (String acao : ACOES) {
            proxima(tipo, situacao, acao).ifPresent(destino -> {
                List<Passo> seguintes = new ArrayList<>(passos);
                seguintes.add(new Passo(acao, destino));
                caminhos.addAll(percorrer(tipo, destino, List.copyOf(seguintes)));
            });
        }
        return caminhos;
    }

    private static Optional<String> proxima(String tipo, String situacao, String acao) {
        return Optional.ofNullable(TRANSICOES.get(tipo).get(situacao).get(acao));
    }

    private static Map<String, String> proximas(String tipo, String situacao) {
        return TRANSICOES.get(tipo).get(situacao);
    }
}
