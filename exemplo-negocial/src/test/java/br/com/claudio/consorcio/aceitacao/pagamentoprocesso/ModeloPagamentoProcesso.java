package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.AUSENTE;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.VALOR_PADRAO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.favorecido;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Stream;
import org.junit.jupiter.params.provider.Arguments;

/**
 * Padrão: Máquina de estados como modelo de teste [Ammann e Offutt].
 * Escrita a partir das regras 2 a 6, 10, 11 e 23 do caso de uso (fatia 1), 12 a 14 (fatia 2, valor e crédito),
 * 15 e 29 (fatia 3, favorecidos), 24 a 28 (fatia 4, alteração com concordância do cliente) e 7, 8, 20 a 22 e 31
 * (fatia 5, pagamento como saga: solicitar só grava e uma rotina agendada fala com a tesouraria) e 16 (fatia 6,
 * alçada: concluir a análise ou a vistoria leva a aprovado ou a aguardando alçada conforme o valor vigente);
 * nada aqui vem do código da aplicação.
 *
 * <p>Alçada (R16): a transição de concluir é CONDICIONAL ao valor vigente. O modelo a trata com duas faixas de valor,
 * {@link Faixa}: "até a alçada" (concluir leva a aprovado) e "acima da alçada" (concluir leva a aguardando alçada).
 * Cada {@link Caminho} tem a faixa do processo que o percorre, e o valor dele não muda no caminho: por isso, na
 * faixa acima da alçada, as ações que mudam o valor vigente (alterar e concordar) não entram no grafo; a decisão
 * depois de uma alteração de valor é conferida à mão, nos testes de valor vigente.
 *
 * <p>"Rotina: tesouraria confirmou" e "rotina: tesouraria recusou" são transições do modelo, não ações de pessoa:
 * só existem em PAGAMENTO_SOLICITADO e nunca entram nas ações aceitas, recusadas ou consultadas.
 */
final class ModeloPagamentoProcesso {

    static final String ABERTO = "ABERTO";
    static final String EM_ANALISE = "EM_ANALISE";
    static final String AGUARDANDO_VISTORIA = "AGUARDANDO_VISTORIA";
    static final String AGUARDANDO_CONCORDANCIA = "AGUARDANDO_CONCORDANCIA";
    static final String AGUARDANDO_ALCADA = "AGUARDANDO_ALCADA";
    static final String PENDENTE_DOCUMENTO = "PENDENTE_DOCUMENTO";
    static final String REPROVADO = "REPROVADO";
    static final String APROVADO = "APROVADO";
    static final String PAGAMENTO_SOLICITADO = "PAGAMENTO_SOLICITADO";
    static final String PAGO = "PAGO";
    static final String CANCELADO = "CANCELADO";

    static final String INICIAR_ANALISE = "iniciar-analise";
    static final String SOLICITAR_DOCUMENTO = "solicitar-documento";
    static final String RECEBER_DOCUMENTO = "receber-documento";
    static final String REPROVAR = "reprovar";
    static final String DEVOLVER_PARA_ANALISE = "devolver-para-analise";
    static final String CONCLUIR_ANALISE = "concluir-analise";
    static final String CONCLUIR_VISTORIA = "concluir-vistoria";
    static final String APROVAR_NA_ALCADA = "aprovar-na-alcada";
    static final String SOLICITAR_PAGAMENTO = "solicitar-pagamento";
    /** O endpoint antigo, que deixou de existir na fatia 5: só serve para provar que responde 404. */
    static final String PAGAR = "pagar";
    static final String CANCELAR = "cancelar";
    static final String ALTERAR = "alterar";
    static final String PROPOR_ALTERACAO = "propor-alteracao";
    static final String CONCORDAR = "concordar";
    static final String DISCORDAR = "discordar";

    static final String SERVICO = "SERVICO";
    static final String IMOBILIARIO = "IMOBILIARIO";

    /** Alçada padrão (R16): até este valor, inclusive, concluir leva a aprovado; acima, a aguardando alçada. */
    static final BigDecimal ALCADA = new BigDecimal("50000.00");

    /** Valor de teste acima da alçada e dentro do crédito padrão da cota (R13). */
    static final BigDecimal VALOR_ACIMA_DA_ALCADA = new BigDecimal("60000.00");

    /** Faixa de valor do processo em relação à alçada (R16). O valor padrão dos testes está na primeira. */
    enum Faixa {
        ATE_A_ALCADA(VALOR_PADRAO, ""),
        ACIMA_DA_ALCADA(VALOR_ACIMA_DA_ALCADA, " (acima da alçada)");

        final BigDecimal valor;
        final String marca;

        Faixa(BigDecimal valor, String marca) {
            this.valor = valor;
            this.marca = marca;
        }

        /** Ações que mudam o valor vigente tiram o processo da faixa: não entram nos caminhos da faixa acima. */
        boolean permite(String acao) {
            return this == ATE_A_ALCADA || !ACOES_QUE_MUDAM_O_VALOR.contains(acao);
        }
    }

    private static final Set<String> ACOES_QUE_MUDAM_O_VALOR = Set.of(ALTERAR, CONCORDAR);

    /** R16: a faixa do valor vigente. Até a alçada, inclusive, é a primeira; acima é a segunda. */
    static Faixa faixaDoValor(BigDecimal valor) {
        return valor.compareTo(ALCADA) <= 0 ? Faixa.ATE_A_ALCADA : Faixa.ACIMA_DA_ALCADA;
    }

    /** R16: a situação a que concluir a análise (serviço) ou a vistoria (imobiliário) leva, com o valor vigente dado. */
    static String decisaoDaAlcada(BigDecimal valorVigente) {
        return faixaDoValor(valorVigente) == Faixa.ATE_A_ALCADA ? APROVADO : AGUARDANDO_ALCADA;
    }

    /** O passo que decide a alçada: concluir a análise no serviço, concluir a vistoria no imobiliário (R16, R3). */
    private static boolean ehDecisaoDeAlcada(String tipo, String situacao, String acao) {
        return SERVICO.equals(tipo) ? EM_ANALISE.equals(situacao) && CONCLUIR_ANALISE.equals(acao)
                : AGUARDANDO_VISTORIA.equals(situacao) && CONCLUIR_VISTORIA.equals(acao);
    }

    /** Resultados que a rotina aplica quando a tesouraria responde (R20, R21): transições do modelo, não pedidos. */
    static final String ROTINA_CONFIRMOU = "rotina:tesouraria-confirmou";
    static final String ROTINA_RECUSOU = "rotina:tesouraria-recusou";

    /** Protocolo e motivo que a tesouraria devolve nos exemplos do caso de uso (R20, R21). */
    static final String PROTOCOLO_DA_TESOURARIA = "778899";
    static final String MOTIVO_DA_TESOURARIA = "conta inválida";

    /** Ações de pessoa (as catorze do quadro situação × ação). */
    private static final List<String> ACOES = List.of(INICIAR_ANALISE, SOLICITAR_DOCUMENTO, RECEBER_DOCUMENTO,
            REPROVAR, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, APROVAR_NA_ALCADA, DEVOLVER_PARA_ANALISE,
            SOLICITAR_PAGAMENTO, CANCELAR, ALTERAR, PROPOR_ALTERACAO, CONCORDAR, DISCORDAR);

    private static final Set<String> ACOES_DA_ROTINA = Set.of(ROTINA_CONFIRMOU, ROTINA_RECUSOU);

    /** Ações de pessoa e da rotina: o que percorre o grafo de caminhos. */
    private static final List<String> TODAS_AS_ACOES = Stream.concat(ACOES.stream(),
            Stream.of(ROTINA_CONFIRMOU, ROTINA_RECUSOU)).toList();

    private static final List<String> SITUACOES = List.of(ABERTO, EM_ANALISE, PENDENTE_DOCUMENTO,
            AGUARDANDO_VISTORIA, AGUARDANDO_ALCADA, AGUARDANDO_CONCORDANCIA, APROVADO, PAGAMENTO_SOLICITADO, PAGO,
            REPROVADO, CANCELADO);

    /** Ações cujo corpo é obrigatório e traz o motivo (R4, R10, R11, R23, R25, R27). */
    private static final Set<String> ACOES_COM_MOTIVO =
            Set.of(SOLICITAR_DOCUMENTO, REPROVAR, DEVOLVER_PARA_ANALISE, CANCELAR, PROPOR_ALTERACAO, DISCORDAR);

    /** Ações cujo corpo traz o novo valor e os novos favorecidos (R24, R25). */
    private static final Set<String> ACOES_COM_VALOR_E_FAVORECIDOS = Set.of(ALTERAR, PROPOR_ALTERACAO);

    /** Ações cujo corpo é opcional e pode trazer uma observação. */
    private static final Set<String> ACOES_COM_OBSERVACAO =
            Set.of(INICIAR_ANALISE, CONCLUIR_ANALISE, CONCLUIR_VISTORIA, RECEBER_DOCUMENTO, CONCORDAR,
                    SOLICITAR_PAGAMENTO, APROVAR_NA_ALCADA);

    /**
     * Transições por tipo e situação. Concluir a análise (serviço) e concluir a vistoria (imobiliário) aparecem aqui
     * levando a APROVADO, que é o destino da faixa "até a alçada"; na faixa acima o destino é AGUARDANDO_ALCADA
     * (ver {@link #proxima(String, Faixa, String, String)}). A linha AGUARDANDO_ALCADA vale para as duas faixas, mas só
     * é alcançada na faixa acima.
     */
    private static final Map<String, Map<String, Map<String, String>>> TRANSICOES = Map.of(
            SERVICO, Map.ofEntries(
                    Map.entry(ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO, ALTERAR, ABERTO)),
                    Map.entry(EM_ANALISE, Map.of(SOLICITAR_DOCUMENTO, PENDENTE_DOCUMENTO, REPROVAR, REPROVADO,
                            CONCLUIR_ANALISE, APROVADO, CANCELAR, CANCELADO,
                            PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA)),
                    Map.entry(PENDENTE_DOCUMENTO, Map.of(RECEBER_DOCUMENTO, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(AGUARDANDO_ALCADA, Map.of(APROVAR_NA_ALCADA, APROVADO, REPROVAR, REPROVADO,
                            DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(AGUARDANDO_CONCORDANCIA, Map.of(CONCORDAR, EM_ANALISE, DISCORDAR, EM_ANALISE,
                            CANCELAR, CANCELADO)),
                    Map.entry(APROVADO, Map.of(SOLICITAR_PAGAMENTO, PAGAMENTO_SOLICITADO,
                            DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(PAGAMENTO_SOLICITADO, Map.of(ROTINA_CONFIRMOU, PAGO, ROTINA_RECUSOU, APROVADO)),
                    Map.entry(PAGO, Map.<String, String>of()),
                    Map.entry(REPROVADO, Map.<String, String>of()),
                    Map.entry(CANCELADO, Map.<String, String>of())),
            IMOBILIARIO, Map.ofEntries(
                    Map.entry(ABERTO, Map.of(INICIAR_ANALISE, EM_ANALISE, CANCELAR, CANCELADO, ALTERAR, ABERTO)),
                    Map.entry(EM_ANALISE, Map.of(SOLICITAR_DOCUMENTO, PENDENTE_DOCUMENTO, REPROVAR, REPROVADO,
                            CONCLUIR_ANALISE, AGUARDANDO_VISTORIA, CANCELAR, CANCELADO,
                            PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA)),
                    Map.entry(PENDENTE_DOCUMENTO, Map.of(RECEBER_DOCUMENTO, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(AGUARDANDO_CONCORDANCIA, Map.of(CONCORDAR, EM_ANALISE, DISCORDAR, EM_ANALISE,
                            CANCELAR, CANCELADO)),
                    Map.entry(AGUARDANDO_VISTORIA, Map.of(CONCLUIR_VISTORIA, APROVADO,
                            DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(AGUARDANDO_ALCADA, Map.of(APROVAR_NA_ALCADA, APROVADO, REPROVAR, REPROVADO,
                            DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(APROVADO, Map.of(SOLICITAR_PAGAMENTO, PAGAMENTO_SOLICITADO,
                            DEVOLVER_PARA_ANALISE, EM_ANALISE, CANCELAR, CANCELADO)),
                    Map.entry(PAGAMENTO_SOLICITADO, Map.of(ROTINA_CONFIRMOU, PAGO, ROTINA_RECUSOU, APROVADO)),
                    Map.entry(PAGO, Map.<String, String>of()),
                    Map.entry(REPROVADO, Map.<String, String>of()),
                    Map.entry(CANCELADO, Map.<String, String>of())));

    /** Um passo do fluxo: a ação pedida e a situação esperada depois dela. */
    record Passo(String acao, String situacao) {
    }

    /** Caminho do início até uma situação, na ordem em que o usuário age. */
    record Caminho(String tipo, Faixa faixa, List<Passo> passos) {

        String situacaoFinal() {
            return passos.isEmpty() ? ABERTO : passos.getLast().situacao();
        }

        /** Valor com que o processo é aberto para percorrer o caminho (R16). */
        BigDecimal valor() {
            return faixa.valor;
        }

        @Override
        public String toString() {
            return tipo + faixa.marca + ": " + ABERTO
                    + passos.stream().map(p -> " -> " + p.situacao()).reduce("", String::concat);
        }
    }

    /** Ação que a regra 5 manda recusar, a partir de uma situação alcançada pelo caminho. */
    record AcaoInvalida(Caminho ate, String acao) {

        @Override
        public String toString() {
            return "%s em %s (%s%s)".formatted(acao, ate.situacaoFinal(), ate.tipo(), ate.faixa().marca);
        }
    }

    /** Transição do modelo: de uma situação, por uma ação, para um destino. */
    record Base(String tipo, Faixa faixa, String situacao, String acao, String destino) {

        @Override
        public String toString() {
            return "%s%s: %s em %s -> %s".formatted(tipo, faixa.marca, acao, situacao, destino);
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
        return switch (acao) {
            case ROTINA_CONFIRMOU -> "CONFIRMAR_PAGAMENTO";
            case ROTINA_RECUSOU -> "RECUSAR_PAGAMENTO";
            default -> acao.toUpperCase().replace('-', '_');
        };
    }

    static boolean ehDaRotina(String acao) {
        return ACOES_DA_ROTINA.contains(acao);
    }

    /** Ações aceitas na situação, pelo modelo. */
    static Set<String> acoesAceitas(String tipo, String situacao) {
        Set<String> aceitas = new java.util.LinkedHashSet<>(proximas(tipo, situacao).keySet());
        aceitas.removeAll(ACOES_DA_ROTINA);
        return aceitas;
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
        comCorpo.addAll(ACOES_COM_VALOR_E_FAVORECIDOS);
        return transicoes(comCorpo).filter(base -> vistas.add(base.acao()));
    }

    /** Uma transição por ação de motivo obrigatório, preferindo serviço. */
    static Stream<Base> umaTransicaoPorAcaoComMotivo() {
        Set<String> vistas = new HashSet<>();
        return transicoesComMotivo().filter(base -> vistas.add(base.acao()));
    }


    /** Ações do caminho que a situação recusa (R5, R28), pelo modelo. */
    static Set<String> acoesRecusadas(String tipo, String situacao) {
        Set<String> recusadas = new java.util.LinkedHashSet<>(ACOES);
        recusadas.removeAll(acoesAceitas(tipo, situacao));
        return recusadas;
    }

    // ---- Alteração de valor e favorecidos (fatia 4: R24 a R28) ----

    /** Novo valor e novos favorecidos pedidos em uma alteração ou proposta. */
    record Alteracao(BigDecimal valor, List<Map<String, Object>> favorecidos) {

        /** A n-ésima alteração do teste: valor e favorecidos sempre diferentes dos anteriores (R12, R13, R15, R29). */
        static Alteracao numero(int n) {
            BigDecimal valor = v("30000.00").subtract(v("1000.00").multiply(BigDecimal.valueOf(n)));
            BigDecimal cartorio = v("500.00");
            return new Alteracao(valor, List.of(
                    favorecido("Vendedor da alteração %d".formatted(n), cpf(1000 + n), "vendedor", valor.subtract(cartorio)),
                    favorecido("Cartório da alteração %d".formatted(n), cnpj(2000 + n), "cartório", cartorio)));
        }
    }

    /** O que o histórico deve registrar para uma ação aceita: o nome e, nas alterações, o valor anterior e o novo (R19). */
    record Registro(String acao, BigDecimal valorAnterior, BigDecimal valorNovo) {
    }

    /**
     * Oráculo dos dados VIGENTES de um processo (R24 a R28): valor e favorecidos que valem, proposta pendente e
     * histórico esperado. O teste pede a ação com {@link #preparar} e, só depois do 200, chama {@link #confirmar}.
     * Recusa não chama {@code confirmar}: nada muda.
     */
    static final class ProcessoEsperado {

        private BigDecimal valor;
        private List<Map<String, Object>> favorecidos;
        private Alteracao proposta;
        private String motivoDaProposta;
        private Alteracao pedido;
        private int pedidos;
        private final List<Registro> historico = new ArrayList<>();

        private ProcessoEsperado(BigDecimal valor, List<Map<String, Object>> favorecidos) {
            this.valor = valor;
            this.favorecidos = favorecidos;
        }

        /** Processo aberto com o valor padrão e dois favorecidos conhecidos. */
        static ProcessoEsperado inicial() {
            return comValor(VALOR_PADRAO);
        }

        /** Processo aberto com o valor dado (acima de 5.000,00) e dois favorecidos: o cartório com 5.000,00 e o vendedor com o resto. */
        static ProcessoEsperado comValor(BigDecimal valor) {
            BigDecimal cartorio = v("5000.00");
            return new ProcessoEsperado(valor, List.of(
                    favorecido("Vendedor inicial", cpf(901), "vendedor", valor.subtract(cartorio)),
                    favorecido("Cartório inicial", cnpj(902), "cartório", cartorio)));
        }

        /** Processo aberto com o valor dado e um favorecido que o recebe inteiro. */
        static ProcessoEsperado comUmFavorecido(BigDecimal valor) {
            return new ProcessoEsperado(valor, List.of(favorecido("Favorecido inicial", cpf(901), "vendedor", valor)));
        }

        BigDecimal valor() {
            return valor;
        }

        List<Map<String, Object>> favorecidos() {
            return favorecidos;
        }

        Alteracao proposta() {
            return proposta;
        }

        String motivoDaProposta() {
            return motivoDaProposta;
        }

        List<Registro> historico() {
            return historico;
        }

        /** Corpo da ação, ou null quando a ação não leva corpo. Guarda o pedido de alteração para {@link #confirmar}. */
        Map<String, Object> preparar(String acao) {
            if (ACOES_COM_VALOR_E_FAVORECIDOS.contains(acao)) {
                pedido = Alteracao.numero(++pedidos);
                return corpoDe(pedido, ACOES_COM_MOTIVO.contains(acao) ? motivoPadrao(acao) : null);
            }
            return exigeMotivo(acao) ? Map.of("motivo", motivoPadrao(acao)) : null;
        }

        /** A ação foi aceita (200): atualiza vigentes, proposta e histórico esperados. */
        void confirmar(String acao) {
            switch (acao) {
                case ALTERAR -> alterar(pedido);
                case PROPOR_ALTERACAO -> propor(pedido, motivoPadrao(acao));
                case CONCORDAR -> concordar();
                case DISCORDAR, CANCELAR -> {
                    historico.add(new Registro(acao, null, null));
                    proposta = null;
                    motivoDaProposta = null;
                }
                default -> historico.add(new Registro(acao, null, null));
            }
        }

        /** R24: a alteração vale na hora. */
        void alterar(Alteracao nova) {
            historico.add(new Registro(ALTERAR, valor, nova.valor()));
            valor = nova.valor();
            favorecidos = nova.favorecidos();
        }

        /** R25: a proposta fica à parte; os vigentes continuam os anteriores. */
        void propor(Alteracao nova, String motivo) {
            historico.add(new Registro(PROPOR_ALTERACAO, valor, nova.valor()));
            proposta = nova;
            motivoDaProposta = motivo;
        }

        /** R26: a proposta passa a valer. */
        void concordar() {
            historico.add(new Registro(CONCORDAR, null, null));
            valor = proposta.valor();
            favorecidos = proposta.favorecidos();
            proposta = null;
            motivoDaProposta = null;
        }
    }

    /** Corpo de alterar (sem motivo) ou de propor-alteracao (com motivo). */
    static Map<String, Object> corpoDe(Alteracao alteracao, Object motivo) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("valor", alteracao.valor());
        corpo.put("favorecidos", alteracao.favorecidos());
        if (motivo != null) {
            corpo.put("motivo", motivo);
        }
        return corpo;
    }

    /**
     * Corpo de uma ação de motivo obrigatório com o motivo dado (ou {@code AUSENTE}); as demais partes do corpo
     * (em propor-alteracao, valor e favorecidos) são válidas, para o motivo ser o único defeito.
     */
    static Map<String, Object> corpoComMotivo(String acao, Object motivo) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        if (ACOES_COM_VALOR_E_FAVORECIDOS.contains(acao)) {
            Alteracao alteracao = Alteracao.numero(1);
            corpo.put("valor", alteracao.valor());
            corpo.put("favorecidos", alteracao.favorecidos());
        }
        corpo.put("motivo", motivo);
        return corpo;
    }

    // ---- Valor e crédito (fatia 2: R12, R13, R14) ----

    /** Processo de um cenário de crédito: tipo, valor e a situação em que o cenário o deixa. */
    record ProcessoPlanejado(String tipo, BigDecimal valor, String situacao) {

        Caminho caminho() {
            return caminhoAte(tipo, situacao, valor);
        }

        boolean consomeCredito() {
            return PAGO.equals(situacao);
        }
    }

    /**
     * Cota com um crédito e os processos que passaram por ela, em ordem. Só o último pode estar em andamento (R6).
     * O oráculo do crédito disponível é R13 com R14: crédito menos o valor dos processos PAGOS; processo em
     * andamento, reprovado ou cancelado não consome nada.
     */
    record CenarioCredito(String nome, BigDecimal credito, List<ProcessoPlanejado> processos) {

        BigDecimal creditoDisponivelEsperado() {
            return processos.stream().filter(ProcessoPlanejado::consomeCredito)
                    .map(ProcessoPlanejado::valor).reduce(credito, BigDecimal::subtract);
        }

        boolean terminaEmAndamento() {
            return !processos.isEmpty() && !proximas(processos.getLast().tipo(), processos.getLast().situacao()).isEmpty();
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private static BigDecimal v(String valor) {
        return new BigDecimal(valor);
    }

    private static ProcessoPlanejado processo(String tipo, String valor, String situacao) {
        return new ProcessoPlanejado(tipo, v(valor), situacao);
    }

    private static CenarioCredito cenario(String nome, String credito, ProcessoPlanejado... processos) {
        return new CenarioCredito(nome, v(credito), List.of(processos));
    }

    /** Cenários em que o último processo é final: a cota está livre para abrir outro (R6, R14). */
    static Stream<CenarioCredito> cenariosComCotaLivre() {
        return Stream.of(
                cenario("cota sem processo", "250000.00"),
                cenario("pago 100000 (serviço)", "250000.00", processo(SERVICO, "100000.00", PAGO)),
                cenario("pago 100000 (imobiliário)", "250000.00", processo(IMOBILIARIO, "100000.00", PAGO)),
                cenario("dois pagos somam: 100000 e 50000", "250000.00",
                        processo(SERVICO, "100000.00", PAGO), processo(IMOBILIARIO, "50000.00", PAGO)),
                cenario("cancelado 100000 não consome (serviço)", "250000.00",
                        processo(SERVICO, "100000.00", CANCELADO)),
                cenario("cancelado 100000 não consome (imobiliário)", "250000.00",
                        processo(IMOBILIARIO, "100000.00", CANCELADO)),
                cenario("reprovado 100000 não consome (serviço)", "250000.00",
                        processo(SERVICO, "100000.00", REPROVADO)),
                cenario("reprovado 100000 não consome (imobiliário)", "250000.00",
                        processo(IMOBILIARIO, "100000.00", REPROVADO)),
                cenario("pago 100000, cancelado 30000 e reprovado 20000: só o pago consome", "250000.00",
                        processo(SERVICO, "100000.00", PAGO), processo(SERVICO, "30000.00", CANCELADO),
                        processo(IMOBILIARIO, "20000.00", REPROVADO)),
                cenario("pago de um centavo", "250000.00", processo(SERVICO, "0.01", PAGO)),
                cenario("crédito com centavos: pago 500,25 de 1000,50", "1000.50", processo(SERVICO, "500.25", PAGO)),
                cenario("crédito esgotado: pago todo o crédito", "250000.00", processo(SERVICO, "250000.00", PAGO)));
    }

    /** Cenários com processo em andamento na cota: ele não consome crédito (R13). */
    static Stream<CenarioCredito> cenariosComProcessoEmAndamento() {
        return Stream.concat(
                caminhosEmAndamento().map(caminho -> cenario(
                        "em andamento de 100000 em " + caminho.situacaoFinal() + " (" + caminho.tipo() + ")",
                        "250000.00", processo(caminho.tipo(), "100000.00", caminho.situacaoFinal()))),
                Stream.of(cenario("pago 100000 e outro de 60000 em análise", "250000.00",
                        processo(SERVICO, "100000.00", PAGO), processo(SERVICO, "60000.00", EM_ANALISE))));
    }

    /** Todos os cenários, para conferir o crédito disponível. */
    static Stream<CenarioCredito> cenariosDeCredito() {
        return Stream.concat(cenariosComCotaLivre(), cenariosComProcessoEmAndamento());
    }

    // ---- Favorecidos (fatia 3: R15, R29, na abertura) ----

    /** O que o modelo manda fazer com um pedido de abertura, olhando só os favorecidos. */
    enum Resultado {
        /** Todas as regras de R15 e R29 cumpridas: 201 e a consulta devolve os favorecidos informados. */
        ACEITO,
        /** Dado de favorecido ausente, em branco, fora do tamanho ou do formato: 400 com {@code campos}. */
        RECUSA_DE_FORMATO,
        /** Dados bem formados, mas a soma das partes difere do valor ou há documento repetido: 400 com mensagem. */
        RECUSA_DE_REGRA
    }

    /**
     * Pedido de abertura com valor e favorecidos. {@code favorecidos} é uma lista de mapas (campos com valor,
     * {@code null} ou {@link br.com.claudio.consorcio.aceitacao.ApiAceitacao#AUSENTE}), ou o próprio
     * {@code AUSENTE} ou {@code null} para a lista inteira.
     * O oráculo {@link #resultado()} aplica R15 e R29 e o contrato (nome até 120, papel até 50, documento com
     * 11 ou 14 dígitos, parte maior que zero com até duas casas); formato é conferido antes de regra.
     */
    record CasoFavorecidos(String nome, BigDecimal valor, Object favorecidos) {

        CasoFavorecidos comValor(BigDecimal outroValor) {
            return new CasoFavorecidos(nome, outroValor, favorecidos);
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> esperados() {
            return (List<Map<String, Object>>) favorecidos;
        }

        Resultado resultado() {
            if (!(favorecidos instanceof List<?> lista) || lista.isEmpty()) {
                return Resultado.RECUSA_DE_FORMATO;
            }
            BigDecimal soma = BigDecimal.ZERO;
            Set<Object> documentos = new HashSet<>();
            boolean repetido = false;
            for (Object item : lista) {
                if (!(item instanceof Map<?, ?> f)) {
                    return Resultado.RECUSA_DE_FORMATO;
                }
                if (!textoValido(f.get("nome"), 120) || !textoValido(f.get("papel"), 50)
                        || !documentoValido(f.get("documento"))) {
                    return Resultado.RECUSA_DE_FORMATO;
                }
                if (!(f.get("valor") instanceof BigDecimal parte) || parte.signum() <= 0
                        || parte.stripTrailingZeros().scale() > 2) {
                    return Resultado.RECUSA_DE_FORMATO;
                }
                soma = soma.add(parte);
                repetido |= !documentos.add(f.get("documento"));
            }
            return soma.compareTo(valor) != 0 || repetido ? Resultado.RECUSA_DE_REGRA : Resultado.ACEITO;
        }

        private static boolean textoValido(Object texto, int maximo) {
            return texto instanceof String s && !s.isBlank() && s.length() <= maximo;
        }

        private static boolean documentoValido(Object documento) {
            return documento instanceof String s && s.matches("[0-9]{11}|[0-9]{14}");
        }

        @Override
        public String toString() {
            return nome;
        }
    }

    private static CasoFavorecidos caso(String nome, String valor, Object favorecidos) {
        return new CasoFavorecidos(nome, v(valor), favorecidos);
    }

    private static Map<String, Object> fav(String nome, String documento, String papel, String parte) {
        return favorecido(nome, documento, papel, v(parte));
    }

    /** CPF sintético (11 dígitos; esta entrega não confere dígito verificador). */
    private static String cpf(int n) {
        return "%011d".formatted(12345678900L + n);
    }

    /** CNPJ sintético (14 dígitos; esta entrega não confere dígito verificador). */
    private static String cnpj(int n) {
        return "%014d".formatted(12345678000100L + n);
    }

    private static final String NOME_ANA = "Ana Lima";
    private static final String NOME_CARTORIO = "Cartório Norte";

    /** Base dos casos de formato e de regra: Ana (CPF) com a parte dada e o cartório (CNPJ) com a parte dada. */
    private static List<Map<String, Object>> parDeFavorecidos(Object parteAna, Object parteCartorio) {
        return List.of(favorecido(NOME_ANA, cpf(1), "vendedor", parteAna),
                favorecido(NOME_CARTORIO, cnpj(2), "cartório", parteCartorio));
    }

    /** Segundo favorecido com o defeito dado; Ana fica com 60,00 e o segundo com 40,00 quando a parte está certa. */
    private static List<Map<String, Object>> segundoComDefeito(Object nome, Object documento, Object papel, Object parte) {
        return List.of(fav(NOME_ANA, cpf(1), "vendedor", "60.00"), favorecido(nome, documento, papel, parte));
    }

    /** n favorecidos distintos (CPF e CNPJ alternados), todos com a mesma parte. */
    private static List<Map<String, Object>> varios(int quantidade, String parte) {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            lista.add(fav("Favorecido %03d".formatted(i), i % 2 == 0 ? cpf(100 + i) : cnpj(100 + i), "papel " + i, parte));
        }
        return lista;
    }

    private static List<Map<String, Object>> variosComUm(int quantidade, String parte, int posicao,
            Map<String, Object> substituto) {
        List<Map<String, Object>> lista = new ArrayList<>(varios(quantidade, parte));
        lista.set(posicao, substituto);
        return lista;
    }

    /** O exemplo do caso de uso (R15): vendedor R$ 9.500,00 e cartório R$ 500,00 em um processo de R$ 10.000,00. */
    static CasoFavorecidos exemploDoCasoDeUso() {
        return caso("exemplo do caso de uso: vendedor 9500,00 e cartório 500,00", "10000.00", List.of(
                fav("João da Silva", "12345678901", "vendedor", "9500.00"),
                fav("Cartório Central", "12345678000199", "cartório", "500.00")));
    }

    /** Favorecidos de um processo acima da alçada (R16): vendedor R$ 55.000,00 e cartório R$ 5.000,00 em R$ 60.000,00. */
    static CasoFavorecidos favorecidosAcimaDaAlcada() {
        return caso("processo acima da alçada: vendedor 55000,00 e cartório 5000,00", "60000.00", List.of(
                fav("João da Silva", "12345678901", "vendedor", "55000.00"),
                fav("Cartório Central", "12345678000199", "cartório", "5000.00")));
    }

    /** Aberturas que R15 e R29 mandam aceitar (partição e limites). */
    static Stream<CasoFavorecidos> favorecidosAceitos() {
        return Stream.of(
                exemploDoCasoDeUso(),
                caso("um favorecido (CPF) com o valor inteiro", "10000.00",
                        List.of(fav("Maria Souza", cpf(1), "vendedor", "10000.00"))),
                caso("um favorecido (CNPJ) com o valor inteiro", "10000.00",
                        List.of(fav("Imobiliária Central Ltda", cnpj(1), "vendedor", "10000.00"))),
                caso("dois favorecidos, metade para cada um", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "5000.00"), fav("Bruno Reis", cpf(2), "vendedor", "5000.00"))),
                caso("dez favorecidos com partes diferentes somando o valor", "5500.00", dezComPartesDiferentes()),
                caso("cem favorecidos de 1,00 somando 100,00 (sem limite máximo)", "100.00", varios(100, "1.00")),
                caso("parte de 0,01 e o resto na outra", "10000.00", parDeFavorecidos(v("0.01"), v("9999.99"))),
                caso("duas partes de 0,01 em um processo de 0,02", "0.02", parDeFavorecidos(v("0.01"), v("0.01"))),
                caso("um favorecido com 0,01 em um processo de 0,01", "0.01",
                        List.of(fav("Ana Lima", cpf(1), "vendedor", "0.01"))),
                caso("rateio com centavos: 33,34 + 33,33 + 33,33 = 100,00", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "33.34"), fav("Bruno Reis", cpf(2), "vendedor", "33.33"),
                        fav("Carla Dias", cpf(3), "vendedor", "33.33"))),
                caso("partes sem casas decimais: 9500 + 500", "10000.00", parDeFavorecidos(v("9500"), v("500"))),
                caso("partes com uma casa decimal: 9500,5 + 499,5", "10000.00",
                        parDeFavorecidos(v("9500.5"), v("499.5"))),
                caso("valor com centavos dividido ao meio: 6250,25 + 6250,25", "12500.50",
                        parDeFavorecidos(v("6250.25"), v("6250.25"))),
                caso("nome e papel com 1 caractere", "100.00", List.of(fav("A", cpf(1), "v", "100.00"))),
                caso("nome com 120 e papel com 50 caracteres (limites)", "100.00",
                        List.of(fav("a".repeat(120), cpf(1), "b".repeat(50), "100.00"))),
                caso("nome com 120 e papel com 50 caracteres acentuados", "100.00",
                        List.of(fav("ã".repeat(120), cpf(1), "é".repeat(50), "100.00"))),
                caso("papel é texto livre: vendedor, cartório, corretor e advogado", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "25.00"), fav("Cartório Norte", cnpj(2), "cartório", "25.00"),
                        fav("Carla Dias", cpf(3), "corretor", "25.00"), fav("Davi Melo", cpf(4), "advogado", "25.00"))),
                caso("mesmo nome e papel com documentos diferentes não é o mesmo favorecido", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "50.00"), fav("Ana Lima", cpf(2), "vendedor", "50.00"))),
                caso("documentos com zeros à esquerda (CPF e CNPJ)", "100.00", List.of(
                        fav("Ana Lima", "00123456789", "vendedor", "50.00"),
                        fav("Cartório Norte", "00123456000100", "cartório", "50.00"))));
    }

    private static List<Map<String, Object>> dezComPartesDiferentes() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (int i = 1; i <= 10; i++) {
            lista.add(fav("Favorecido %02d".formatted(i), i % 2 == 0 ? cnpj(i) : cpf(i), "papel " + i, "%d00.00".formatted(i)));
        }
        return lista;
    }

    /** Lista de favorecidos informada em ordem que nenhuma chave (nome, documento, papel, parte) reproduz sozinha. */
    static CasoFavorecidos favorecidosEmOrdemEmbaralhada() {
        return caso("quatro favorecidos fora de ordem alfabética, de documento e de parte", "1000.00", List.of(
                fav("Zélia Prado", cpf(50), "vendedor", "100.50"),
                fav("Ana Alves", cnpj(10), "cartório", "400.00"),
                fav("Marcos Dias", cpf(30), "corretor", "250.25"),
                fav("Beto Nunes", cnpj(70), "advogado", "249.25")));
    }

    /** Aberturas recusadas por erro de FORMATO nos favorecidos: 400 com {@code campos} (R15, R29 e o contrato). */
    static Stream<CasoFavorecidos> favorecidosRecusadosPorFormato() {
        String docB = cnpj(2);
        return Stream.of(
                // Lista
                caso("lista ausente", "100.00", AUSENTE),
                caso("lista nula", "100.00", null),
                caso("lista vazia", "100.00", List.of()),
                // Nome
                caso("nome ausente", "100.00", segundoComDefeito(AUSENTE, docB, "cartório", v("40.00"))),
                caso("nome nulo", "100.00", segundoComDefeito(null, docB, "cartório", v("40.00"))),
                caso("nome vazio", "100.00", segundoComDefeito("", docB, "cartório", v("40.00"))),
                caso("nome só com espaços", "100.00", segundoComDefeito("   ", docB, "cartório", v("40.00"))),
                caso("nome com 121 caracteres", "100.00",
                        segundoComDefeito("a".repeat(121), docB, "cartório", v("40.00"))),
                // Documento
                caso("documento ausente", "100.00", segundoComDefeito("Cartório Norte", AUSENTE, "cartório", v("40.00"))),
                caso("documento nulo", "100.00", segundoComDefeito("Cartório Norte", null, "cartório", v("40.00"))),
                caso("documento vazio", "100.00", segundoComDefeito("Cartório Norte", "", "cartório", v("40.00"))),
                caso("documento só com espaços", "100.00",
                        segundoComDefeito("Cartório Norte", "           ", "cartório", v("40.00"))),
                caso("documento com 1 dígito", "100.00", segundoComDefeito("Cartório Norte", "1", "cartório", v("40.00"))),
                caso("documento com 10 dígitos", "100.00",
                        segundoComDefeito("Cartório Norte", "1234567890", "cartório", v("40.00"))),
                caso("documento com 12 dígitos", "100.00",
                        segundoComDefeito("Cartório Norte", "123456789012", "cartório", v("40.00"))),
                caso("documento com 13 dígitos", "100.00",
                        segundoComDefeito("Cartório Norte", "1234567800019", "cartório", v("40.00"))),
                caso("documento com 15 dígitos", "100.00",
                        segundoComDefeito("Cartório Norte", "123456780001999", "cartório", v("40.00"))),
                caso("CPF com pontuação", "100.00",
                        segundoComDefeito("Cartório Norte", "123.456.789-01", "cartório", v("40.00"))),
                caso("CNPJ com pontuação", "100.00",
                        segundoComDefeito("Cartório Norte", "12.345.678/0001-99", "cartório", v("40.00"))),
                caso("documento de 11 caracteres com uma letra", "100.00",
                        segundoComDefeito("Cartório Norte", "1234567890a", "cartório", v("40.00"))),
                caso("documento de 11 letras", "100.00",
                        segundoComDefeito("Cartório Norte", "abcdefghijk", "cartório", v("40.00"))),
                caso("documento de 11 caracteres com espaço no meio", "100.00",
                        segundoComDefeito("Cartório Norte", "1234 678901", "cartório", v("40.00"))),
                caso("documento de 11 caracteres com sinal", "100.00",
                        segundoComDefeito("Cartório Norte", "-2345678901", "cartório", v("40.00"))),
                caso("documento de 14 caracteres com uma letra", "100.00",
                        segundoComDefeito("Cartório Norte", "1234567800019X", "cartório", v("40.00"))),
                // Papel
                caso("papel ausente", "100.00", segundoComDefeito("Cartório Norte", docB, AUSENTE, v("40.00"))),
                caso("papel nulo", "100.00", segundoComDefeito("Cartório Norte", docB, null, v("40.00"))),
                caso("papel vazio", "100.00", segundoComDefeito("Cartório Norte", docB, "", v("40.00"))),
                caso("papel só com espaços", "100.00", segundoComDefeito("Cartório Norte", docB, "  ", v("40.00"))),
                caso("papel com 51 caracteres", "100.00",
                        segundoComDefeito("Cartório Norte", docB, "b".repeat(51), v("40.00"))),
                // Parte (a soma continua certa, para o defeito ser só o formato da parte)
                caso("parte ausente", "100.00", parDeFavorecidos(v("100.00"), AUSENTE)),
                caso("parte nula", "100.00", parDeFavorecidos(v("100.00"), null)),
                caso("parte zero", "100.00", parDeFavorecidos(v("100.00"), v("0"))),
                caso("parte 0,00", "100.00", parDeFavorecidos(v("100.00"), v("0.00"))),
                caso("parte negativa de 0,01", "100.00", parDeFavorecidos(v("100.01"), v("-0.01"))),
                caso("parte negativa de 40,00", "100.00", parDeFavorecidos(v("140.00"), v("-40.00"))),
                caso("parte com três casas: 40,005", "100.00", parDeFavorecidos(v("59.995"), v("40.005"))),
                caso("parte com três casas: 0,001", "100.00", parDeFavorecidos(v("99.999"), v("0.001"))),
                caso("parte do primeiro favorecido com três casas: 60,005", "100.00",
                        parDeFavorecidos(v("60.005"), v("39.995"))),
                // Posição do defeito
                caso("defeito no primeiro de dois favorecidos (sem nome)", "100.00", List.of(
                        favorecido(AUSENTE, cpf(1), "vendedor", v("60.00")), fav(NOME_CARTORIO, docB, "cartório", "40.00"))),
                caso("defeito no décimo de dez favorecidos (documento com 10 dígitos)", "100.00",
                        variosComUm(10, "10.00", 9, fav("Favorecido 9", "1234567890", "papel 9", "10.00"))));
    }

    /** Aberturas recusadas por REGRA: soma das partes diferente do valor, ou mesmo documento repetido. */
    static Stream<CasoFavorecidos> favorecidosRecusadosPorRegra() {
        Map<String, Object> primeiro = varios(10, "10.00").getFirst();
        return Stream.of(
                // Soma
                caso("exemplo do caso de uso: 9500,00 + 400,00 = 9900,00 de 10000,00", "10000.00",
                        parDeFavorecidos(v("9500.00"), v("400.00"))),
                caso("soma um centavo abaixo: 9500,00 + 499,99", "10000.00", parDeFavorecidos(v("9500.00"), v("499.99"))),
                caso("soma um centavo acima: 9500,00 + 500,01", "10000.00", parDeFavorecidos(v("9500.00"), v("500.01"))),
                caso("soma bem acima: 9500,00 + 600,00", "10000.00", parDeFavorecidos(v("9500.00"), v("600.00"))),
                caso("um favorecido um centavo abaixo do valor", "10000.00",
                        List.of(fav("Ana Lima", cpf(1), "vendedor", "9999.99"))),
                caso("um favorecido um centavo acima do valor", "10000.00",
                        List.of(fav("Ana Lima", cpf(1), "vendedor", "10000.01"))),
                caso("um favorecido com 1,00 em um processo de 10000,00", "10000.00",
                        List.of(fav("Ana Lima", cpf(1), "vendedor", "1.00"))),
                caso("rateio com centavos: 33,33 x 3 = 99,99 de 100,00", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "33.33"), fav("Bruno Reis", cpf(2), "vendedor", "33.33"),
                        fav("Carla Dias", cpf(3), "vendedor", "33.33"))),
                caso("rateio com centavos: 33,34 x 3 = 100,02 de 100,00", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "33.34"), fav("Bruno Reis", cpf(2), "vendedor", "33.34"),
                        fav("Carla Dias", cpf(3), "vendedor", "33.34"))),
                caso("duas partes de 0,01 e 9999,98 somam 9999,99 de 10000,00", "10000.00",
                        parDeFavorecidos(v("0.01"), v("9999.98"))),
                caso("dez favorecidos, soma um centavo abaixo (nove de 10,00 e um de 9,99)", "100.00",
                        variosComUm(10, "10.00", 9, fav("Favorecido 9", cpf(109), "papel 9", "9.99"))),
                caso("dez favorecidos, soma um centavo acima (nove de 10,00 e um de 10,01)", "100.00",
                        variosComUm(10, "10.00", 9, fav("Favorecido 9", cpf(109), "papel 9", "10.01"))),
                // Documento repetido (a soma está certa)
                caso("mesmo CPF duas vezes, com o mesmo nome e papel", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "6000.00"), fav("Ana Lima", cpf(1), "vendedor", "4000.00"))),
                caso("mesmo CPF duas vezes, com nome e papel diferentes", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "6000.00"), fav("Ana L. Souza", cpf(1), "cartório", "4000.00"))),
                caso("mesmo CPF duas vezes, com partes iguais", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "5000.00"), fav("Ana Lima", cpf(1), "vendedor", "5000.00"))),
                caso("mesmo CNPJ duas vezes", "10000.00", List.of(
                        fav("Cartório Norte", cnpj(2), "cartório", "500.00"),
                        fav("Cartório Norte", cnpj(2), "cartório", "9500.00"))),
                caso("mesmo CPF no primeiro e no terceiro de três favorecidos", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "5000.00"), fav("Bruno Reis", cpf(2), "vendedor", "3000.00"),
                        fav("Ana Lima", cpf(1), "vendedor", "2000.00"))),
                caso("mesmo CPF três vezes", "10000.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "5000.00"), fav("Ana Lima", cpf(1), "vendedor", "3000.00"),
                        fav("Ana Lima", cpf(1), "vendedor", "2000.00"))),
                caso("mesmo documento no primeiro e no décimo de dez favorecidos", "100.00",
                        variosComUm(10, "10.00", 9, favorecido("Outro Nome", primeiro.get("documento"), "outro papel", v("10.00")))));
    }

    /**
     * Tabela de decisão de R15 e R29 na abertura: o caso-base (Ana 60,00 e o cartório 40,00 em um processo de 100,00)
     * é aceito, e cada condição, sozinha, recusa. O oráculo diz o resultado de cada linha.
     */
    static Stream<CasoFavorecidos> tabelaDeDecisao() {
        return Stream.of(
                caso("base: todas as condições cumpridas", "100.00", parDeFavorecidos(v("60.00"), v("40.00"))),
                caso("só a condição 'ao menos um favorecido' falha: lista vazia", "100.00", List.of()),
                caso("só a condição 'dados completos' falha: segundo sem papel", "100.00",
                        segundoComDefeito(NOME_CARTORIO, cnpj(2), AUSENTE, v("40.00"))),
                caso("só a condição 'parte maior que zero' falha: partes 100,00 e 0,00", "100.00",
                        parDeFavorecidos(v("100.00"), v("0.00"))),
                caso("só a condição 'soma exata' falha: 60,00 e 39,99", "100.00",
                        parDeFavorecidos(v("60.00"), v("39.99"))),
                caso("só a condição 'sem repetição' falha: mesmo CPF nos dois", "100.00", List.of(
                        fav("Ana Lima", cpf(1), "vendedor", "60.00"), fav("Ana Lima", cpf(1), "vendedor", "40.00"))));
    }

    /** Recusas de formato usadas na precedência (400 antes de 404 e 409): lista, dado do favorecido e parte. */
    static Stream<CasoFavorecidos> favorecidosRecusadosPorFormatoRepresentativos() {
        List<String> nomes = List.of("lista ausente", "lista vazia", "documento com 10 dígitos", "parte 0,00");
        return favorecidosRecusadosPorFormato().filter(caso -> nomes.contains(caso.nome()));
    }

    // ---- Pagamento como saga (fatia 5: R7, R8, R20, R21, R22, R31) ----

    /**
     * Tabela de decisão da resposta da tesouraria à rotina: o que cada resposta faz com o processo em pagamento
     * solicitado. {@code acaoDoResultado} é a entrada que a rotina põe no histórico quando APLICA a resposta
     * (nula quando nada é aplicado e só se registra a tentativa).
     */
    enum Resposta {
        CONFIRMOU("confirma com protocolo", PAGO, "CONFIRMAR_PAGAMENTO"),
        JA_PROCESSOU("diz que já fez a ordem e informa o protocolo (vale como confirmação)", PAGO,
                "CONFIRMAR_PAGAMENTO"),
        RECUSOU("recusa com motivo", APROVADO, "RECUSAR_PAGAMENTO"),
        INDISPONIVEL("está indisponível (erro do servidor)", PAGAMENTO_SOLICITADO, null),
        DEMOROU("demora mais de 2 segundos para responder", PAGAMENTO_SOLICITADO, null),
        CONFIRMACAO_SEM_PROTOCOLO("confirma sem protocolo (incoerente, R31)", PAGAMENTO_SOLICITADO, null),
        RECUSA_SEM_MOTIVO("recusa sem motivo (incoerente, R31)", PAGAMENTO_SOLICITADO, null);

        final String descricao;
        final String situacaoDepois;
        final String acaoDoResultado;

        Resposta(String descricao, String situacaoDepois, String acaoDoResultado) {
            this.descricao = descricao;
            this.situacaoDepois = situacaoDepois;
            this.acaoDoResultado = acaoDoResultado;
        }

        /** A resposta tem efeito: o processo sai de pagamento solicitado. */
        boolean aplicada() {
            return acaoDoResultado != null;
        }

        /** Só o processo PAGO tem protocolo e consome crédito (R13, R20). */
        boolean paga() {
            return PAGO.equals(situacaoDepois);
        }

        /** Protocolo esperado no processo e no histórico. */
        String protocolo() {
            return paga() ? PROTOCOLO_DA_TESOURARIA : null;
        }

        @Override
        public String toString() {
            return "a tesouraria " + descricao;
        }
    }

    /** Cada resposta no serviço, e as duas que decidem o destino também no imobiliário. */
    static Stream<Arguments> respostasPorTipo() {
        return Stream.concat(
                java.util.Arrays.stream(Resposta.values()).map(r -> Arguments.of(r, SERVICO)),
                Stream.of(Resposta.CONFIRMOU, Resposta.RECUSOU).map(r -> Arguments.of(r, IMOBILIARIO)));
    }

    /**
     * Cenários com a cota livre que bastam para o valor-limite do crédito: o limite (R13) não depende do tipo nem
     * de quantos processos foram pagos, e cada processo pago custa a espera da rotina. Ficam: sem processo, um pago,
     * dois pagos, um cancelado, um reprovado, o crédito com centavos e o crédito esgotado.
     */
    static Stream<CenarioCredito> cenariosComCotaLivreAmostrados() {
        List<String> amostra = List.of("cota sem processo", "pago 100000 (serviço)",
                "dois pagos somam: 100000 e 50000", "cancelado 100000 não consome (serviço)",
                "reprovado 100000 não consome (serviço)", "crédito com centavos: pago 500,25 de 1000,50",
                "crédito esgotado: pago todo o crédito");
        return cenariosComCotaLivre().filter(c -> amostra.contains(c.nome()));
    }

    private static Stream<Base> transicoes(Set<String> acoes) {
        return tipos().flatMap(tipo -> SITUACOES.stream().flatMap(situacao -> ACOES.stream()
                .filter(acoes::contains)
                .flatMap(acao -> Stream.of(Faixa.values())
                        .filter(faixa -> valeNaFaixa(tipo, faixa, situacao, acao))
                        .flatMap(faixa -> proxima(tipo, faixa, situacao, acao)
                                .map(destino -> new Base(tipo, faixa, situacao, acao, destino)).stream()))));
    }

    /**
     * A transição entra nos casos da faixa? Aguardando alçada só existe acima da alçada; a decisão de concluir tem uma
     * transição em cada faixa (aprovado ou aguardando alçada); o resto é igual nas duas faixas e fica na primeira.
     */
    private static boolean valeNaFaixa(String tipo, Faixa faixa, String situacao, String acao) {
        if (AGUARDANDO_ALCADA.equals(situacao)) {
            return faixa == Faixa.ACIMA_DA_ALCADA;
        }
        return ehDecisaoDeAlcada(tipo, situacao, acao) || faixa == Faixa.ATE_A_ALCADA;
    }

    /**
     * Menor caminho do início até a situação. Para aguardando alçada o processo está acima da alçada; para as demais
     * situações, até a alçada.
     */
    static Caminho caminhoAte(String tipo, String situacao) {
        return caminhoAte(tipo, AGUARDANDO_ALCADA.equals(situacao) ? Faixa.ACIMA_DA_ALCADA : Faixa.ATE_A_ALCADA,
                situacao);
    }

    /** Menor caminho até a situação para um processo com o valor dado: a faixa do valor decide se concluir passa pela alçada. */
    static Caminho caminhoAte(String tipo, String situacao, BigDecimal valor) {
        return caminhoAte(tipo, faixaDoValor(valor), situacao);
    }

    static Caminho caminhoAte(String tipo, Faixa faixa, String situacao) {
        return primeiroCaminhoAte(tipo, faixa, situacao);
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
        return todosOsCaminhosCompletos().filter(caminho -> !usaAcaoDeAlteracao(caminho));
    }

    private static Stream<Caminho> todosOsCaminhosCompletos() {
        return tipos().flatMap(tipo -> percorrer(tipo, Faixa.ATE_A_ALCADA, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> proximas(tipo, caminho.situacaoFinal()).isEmpty()));
    }

    private static final Set<String> ACOES_DE_ALTERACAO = Set.of(ALTERAR, PROPOR_ALTERACAO, CONCORDAR, DISCORDAR);

    private static boolean usaAcaoDeAlteracao(Caminho caminho) {
        return caminho.passos().stream().anyMatch(p -> ACOES_DE_ALTERACAO.contains(p.acao()));
    }

    /**
     * Caminhos completos que passam por alterar, propor, concordar ou discordar (fatia 4). Todos somam centenas
     * (o grafo tem ciclos), então escolhe-se, para cada tipo, um conjunto mínimo (guloso) que usa cada transição
     * de alteração (inclusive cancelar em aguardando concordância) e chega a cada situação final depois de uma
     * alteração. Os caminhos que não usam ação de alteração ficam em {@link #caminhosCompletos()}.
     */
    static Stream<Caminho> caminhosCompletosComAlteracao() {
        return tipos().flatMap(tipo -> {
            List<Caminho> candidatos = todosOsCaminhosCompletos()
                    .filter(c -> c.tipo().equals(tipo) && usaAcaoDeAlteracao(c)).toList();
            Set<String> faltam = new HashSet<>();
            candidatos.forEach(c -> faltam.addAll(coberturaDeAlteracao(c)));
            List<Caminho> escolhidos = new ArrayList<>();
            while (!faltam.isEmpty()) {
                Caminho melhor = null;
                int melhorGanho = 0;
                for (Caminho candidato : candidatos) {
                    Set<String> ganho = new HashSet<>(coberturaDeAlteracao(candidato));
                    ganho.retainAll(faltam);
                    if (ganho.size() > melhorGanho) {
                        melhor = candidato;
                        melhorGanho = ganho.size();
                    }
                }
                escolhidos.add(melhor);
                faltam.removeAll(coberturaDeAlteracao(melhor));
            }
            return escolhidos.stream();
        });
    }

    /** Transições de alteração (e cancelar em aguardando concordância) usadas pelo caminho, e o final alcançado. */
    private static Set<String> coberturaDeAlteracao(Caminho caminho) {
        Set<String> itens = new HashSet<>();
        String antes = ABERTO;
        for (Passo passo : caminho.passos()) {
            if (ACOES_DE_ALTERACAO.contains(passo.acao()) || AGUARDANDO_CONCORDANCIA.equals(antes)) {
                itens.add(antes + "/" + passo.acao());
            }
            antes = passo.situacao();
        }
        itens.add("final " + caminho.situacaoFinal());
        return itens;
    }

    /**
     * Caminhos completos de um processo ACIMA da alçada (fatia 6, R16, R11, R23, R4): passam por aguardando alçada e
     * saem dela por cada uma das quatro ações. Todos somam muitos (o grafo tem ciclos), então escolhe-se, para cada
     * tipo, um conjunto mínimo (guloso, dos caminhos mais curtos para os mais longos) que usa a decisão de concluir
     * que vai a aguardando alçada, cada saída de aguardando alçada e chega a cada situação final depois dela.
     * Nenhum caminho altera o valor (ver {@link Faixa#permite}).
     */
    static Stream<Caminho> caminhosCompletosPelaAlcada() {
        return tipos().flatMap(tipo -> {
            List<Caminho> candidatos = percorrer(tipo, Faixa.ACIMA_DA_ALCADA, ABERTO, List.of(), Set.of()).stream()
                    .filter(c -> proximas(tipo, c.situacaoFinal()).isEmpty())
                    .filter(c -> !coberturaDaAlcada(c).isEmpty())
                    .sorted(java.util.Comparator.comparingInt(c -> c.passos().size())).toList();
            Set<String> faltam = new HashSet<>();
            candidatos.forEach(c -> faltam.addAll(coberturaDaAlcada(c)));
            List<Caminho> escolhidos = new ArrayList<>();
            while (!faltam.isEmpty()) {
                Caminho melhor = null;
                int melhorGanho = 0;
                for (Caminho candidato : candidatos) {
                    Set<String> ganho = new HashSet<>(coberturaDaAlcada(candidato));
                    ganho.retainAll(faltam);
                    if (ganho.size() > melhorGanho) {
                        melhor = candidato;
                        melhorGanho = ganho.size();
                    }
                }
                escolhidos.add(melhor);
                faltam.removeAll(coberturaDaAlcada(melhor));
            }
            return escolhidos.stream();
        });
    }

    /** A decisão que leva a aguardando alçada, cada saída dela e os finais alcançados depois dela. */
    private static Set<String> coberturaDaAlcada(Caminho caminho) {
        Set<String> itens = new HashSet<>();
        String antes = ABERTO;
        boolean passouPelaAlcada = false;
        for (Passo passo : caminho.passos()) {
            if (AGUARDANDO_ALCADA.equals(antes) || AGUARDANDO_ALCADA.equals(passo.situacao())) {
                itens.add(antes + "/" + passo.acao());
            }
            passouPelaAlcada |= AGUARDANDO_ALCADA.equals(passo.situacao());
            antes = passo.situacao();
        }
        if (passouPelaAlcada) {
            itens.add("final " + caminho.situacaoFinal());
        }
        return itens;
    }

    /** Cada par situação × ação que não é uma transição permitida (aguardando alçada com o processo acima da alçada). */
    static Stream<AcaoInvalida> acoesInvalidas() {
        Stream<AcaoInvalida> ateAAlcada = tipos().flatMap(tipo -> percorrer(tipo, Faixa.ATE_A_ALCADA, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.equals(primeiroCaminhoAte(tipo, Faixa.ATE_A_ALCADA, caminho.situacaoFinal())))
                .flatMap(caminho -> ACOES.stream()
                        .filter(acao -> proxima(tipo, caminho.situacaoFinal(), acao).isEmpty())
                        .map(acao -> new AcaoInvalida(caminho, acao))));
        Stream<AcaoInvalida> emAguardandoAlcada = tipos().flatMap(tipo -> {
            Caminho caminho = primeiroCaminhoAte(tipo, Faixa.ACIMA_DA_ALCADA, AGUARDANDO_ALCADA);
            return ACOES.stream().filter(acao -> proxima(tipo, AGUARDANDO_ALCADA, acao).isEmpty())
                    .map(acao -> new AcaoInvalida(caminho, acao));
        });
        return Stream.concat(ateAAlcada, emAguardandoAlcada);
    }

    /** Situações em que o processo ainda está em andamento (regra 6), com o caminho para chegar a elas. */
    static Stream<Caminho> caminhosEmAndamento() {
        return caminhosUnicos().filter(c -> !proximas(c.tipo(), c.situacaoFinal()).isEmpty());
    }

    /** Situações finais (regra 4), com o caminho para chegar a elas. */
    static Stream<Caminho> caminhosFinalizados() {
        return caminhosUnicos().filter(c -> proximas(c.tipo(), c.situacaoFinal()).isEmpty());
    }

    /** Um menor caminho para cada situação de cada tipo; aguardando alçada, com o processo acima da alçada. */
    private static Stream<Caminho> caminhosUnicos() {
        Stream<Caminho> ateAAlcada = tipos().flatMap(tipo -> percorrer(tipo, Faixa.ATE_A_ALCADA, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.equals(primeiroCaminhoAte(tipo, Faixa.ATE_A_ALCADA, caminho.situacaoFinal()))));
        Stream<Caminho> emAguardandoAlcada = tipos()
                .map(tipo -> primeiroCaminhoAte(tipo, Faixa.ACIMA_DA_ALCADA, AGUARDANDO_ALCADA));
        return Stream.concat(ateAAlcada, emAguardandoAlcada);
    }

    private static Caminho primeiroCaminhoAte(String tipo, Faixa faixa, String situacao) {
        return percorrer(tipo, faixa, ABERTO, List.of(), Set.of()).stream()
                .filter(caminho -> caminho.situacaoFinal().equals(situacao))
                .min(java.util.Comparator.comparingInt(caminho -> caminho.passos().size()))
                .orElseThrow(() -> new IllegalStateException(
                        "Sem caminho até %s para %s%s".formatted(situacao, tipo, faixa.marca)));
    }

    /** Busca em profundidade; devolve o caminho atual e todos os que partem dele, sem repetir transição. */
    private static List<Caminho> percorrer(String tipo, Faixa faixa, String situacao, List<Passo> passos,
            Set<String> usadas) {
        List<Caminho> caminhos = new ArrayList<>();
        caminhos.add(new Caminho(tipo, faixa, passos));
        for (String acao : TODAS_AS_ACOES) {
            String transicao = situacao + "/" + acao;
            if (usadas.contains(transicao) || !faixa.permite(acao)) {
                continue;
            }
            proxima(tipo, faixa, situacao, acao).ifPresent(destino -> {
                List<Passo> seguintes = new ArrayList<>(passos);
                seguintes.add(new Passo(acao, destino));
                Set<String> novas = new HashSet<>(usadas);
                novas.add(transicao);
                caminhos.addAll(percorrer(tipo, faixa, destino, List.copyOf(seguintes), Set.copyOf(novas)));
            });
        }
        return caminhos;
    }

    /** Destino da ação na situação, para um processo da faixa: a decisão de concluir depende da faixa (R16). */
    private static Optional<String> proxima(String tipo, Faixa faixa, String situacao, String acao) {
        Optional<String> destino = proxima(tipo, situacao, acao);
        if (faixa == Faixa.ACIMA_DA_ALCADA && ehDecisaoDeAlcada(tipo, situacao, acao)) {
            return Optional.of(AGUARDANDO_ALCADA);
        }
        return destino;
    }

    /** Destino da ação na situação para um processo até a alçada (a linha aguardando alçada vale nas duas faixas). */
    private static Optional<String> proxima(String tipo, String situacao, String acao) {
        return Optional.ofNullable(proximas(tipo, situacao).get(acao));
    }

    private static Map<String, String> proximas(String tipo, String situacao) {
        // Situação que o tipo não tem (ex.: aguardando vistoria no serviço) não aceita ação nenhuma.
        return TRANSICOES.get(tipo).getOrDefault(situacao, Map.of());
    }
}
