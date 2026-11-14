package br.com.claudio.consorcio.aceitacao;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste.Pessoa;
import io.restassured.specification.RequestSpecification;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;
import org.hamcrest.Matcher;

/**
 * Padrão: Test Utility Method [xUnit Test Patterns].
 * Fala com a aplicação somente por HTTP, usando os caminhos e campos do contrato publicado.
 *
 * <p>Identidade (fatia 7, R18): cada chamada leva, por padrão, um token com o papel certo para a ação (quadro "Quem
 * pode executar cada ação"): cadastros e consultas, backoffice "ana"; reprovar e devolver para análise, "carla"
 * (backoffice e aprovador, porque o papel certo depende da situação, que o auxiliar não consulta); aprovar na
 * alçada, aprovador "bruno" (pessoa diferente de quem concluiu a análise, R17); solicitar pagamento, tesouraria
 * "tereza"; abrir, alterar, concordar e discordar, o cliente dono da cota. O auxiliar conhece o dono porque foi ele
 * quem criou a cota. As variantes {@code ...Como} recebem o cabeçalho {@code Authorization} (ou
 * {@link IdentidadeTeste#SEM_TOKEN}) para os testes de papéis.
 */
public final class ApiAceitacao {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    private ApiAceitacao() {
    }

    // ---- Identidade (fatia 7: R17, R18, R19) ----

    /** Cliente de um cadastro que não é de nenhuma cota conhecida: serve a pedidos sobre cota ou processo inexistente. */
    private static final Pessoa CLIENTE_SEM_COTA = Pessoa.cliente(987_654_321L);

    private static final Map<Long, Pessoa> CLIENTE_DA_COTA = new ConcurrentHashMap<>();
    private static final Map<Long, Long> COTA_DO_PROCESSO = new ConcurrentHashMap<>();

    /** O cliente dono da cota criada por {@link #novaCota()}. */
    public static Pessoa clienteDaCota(long cota) {
        Pessoa cliente = CLIENTE_DA_COTA.get(cota);
        return cliente != null ? cliente : CLIENTE_SEM_COTA;
    }

    /** O cliente dono da cota do processo; para processo desconhecido, um cliente sem cota. */
    public static Pessoa clienteDoProcesso(long processo) {
        Long cota = COTA_DO_PROCESSO.get(processo);
        if (cota == null) {
            cota = cotaDoProcessoPelaListagem(processo);
        }
        return cota == null ? CLIENTE_SEM_COTA : clienteDaCota(cota);
    }

    private static Long cotaDoProcessoPelaListagem(long processo) {
        try {
            long cota = numero(consultarProcesso(processo), "cotaId");
            COTA_DO_PROCESSO.put(processo, cota);
            return cota;
        } catch (AssertionError | RuntimeException e) {
            return null;
        }
    }

    /** Papel certo, por padrão, para a ação de pessoa sobre o processo (quadro de papéis do caso de uso). */
    public static String autorizacaoPadraoDaAcao(long processo, String acao) {
        return switch (acao) {
            case "alterar", "concordar", "discordar" -> clienteDoProcesso(processo).autorizacao();
            case "aprovar-na-alcada" -> IdentidadeTeste.BRUNO.autorizacao();
            case "reprovar", "devolver-para-analise" -> IdentidadeTeste.CARLA.autorizacao();
            case "solicitar-pagamento", "pagar" -> IdentidadeTeste.TEREZA.autorizacao();
            default -> IdentidadeTeste.ANA.autorizacao();
        };
    }

    private static String autorizacaoDeConsulta() {
        return IdentidadeTeste.ANA.autorizacao();
    }

    /** Pedido com o cabeçalho de identidade dado; {@link IdentidadeTeste#SEM_TOKEN} não envia cabeçalho nenhum. */
    public static RequestSpecification requisicao(String autorizacao) {
        RequestSpecification pedido = given();
        return autorizacao == null ? pedido : pedido.header("Authorization", autorizacao);
    }

    /** Pedido de consulta com o token padrão (backoffice). */
    public static RequestSpecification requisicaoDeConsulta() {
        return requisicao(autorizacaoDeConsulta());
    }

    /** Pedido com o token de um cliente qualquer (sem cota), para os pedidos de abertura que não dependem do dono. */
    public static RequestSpecification requisicaoDeCliente() {
        return requisicao(CLIENTE_SEM_COTA.autorizacao());
    }

    /** Dono da cota citada no corpo do pedido de abertura; sem cota conhecida, um cliente sem cota. */
    private static String autorizacaoPadraoDaAbertura(Object corpo) {
        Object cota = null;
        try {
            if (corpo instanceof Map<?, ?> mapa) {
                cota = mapa.get("cotaId");
            } else if (corpo instanceof CharSequence texto) {
                cota = new JsonPath(texto.toString()).get("cotaId");
            }
        } catch (RuntimeException e) {
            // corpo malformado: o teste quer a recusa de formato, de qualquer cliente
        }
        return (cota instanceof Number numero ? clienteDaCota(numero.longValue()) : CLIENTE_SEM_COTA).autorizacao();
    }

    private static Long cotaDoCorpo(Object corpo) {
        try {
            Object cota = corpo instanceof Map<?, ?> mapa ? mapa.get("cotaId")
                    : new JsonPath(corpo.toString()).get("cotaId");
            return cota instanceof Number numero ? numero.longValue() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Só para as pré-condições de cadastro de cliente e cota, cujo código exato de sucesso ainda não foi definido. */
    public static Matcher<Integer> sucesso() {
        return allOf(greaterThanOrEqualTo(200), lessThan(300));
    }

    /** Crédito da cota criada por {@link #novaCota()}. */
    public static final BigDecimal CREDITO_PADRAO = new BigDecimal("250000.00");

    /** Valor válido (R12) e dentro do crédito de {@link #CREDITO_PADRAO} (R13), para os casos em que o valor não é o assunto. */
    public static final BigDecimal VALOR_PADRAO = new BigDecimal("30000.00");

    public static long novaCota() {
        return novaCota(CREDITO_PADRAO);
    }

    public static long novaCota(BigDecimal credito) {
        long cliente = enviar("/clientes", Map.of("nome", "Cliente de Aceitação", "cpf", "%011d".formatted(unico())))
                .statusCode(sucesso()).extract().jsonPath().getLong("id");
        long cota = enviar("/cotas", Map.of("numero", "COTA-" + unico(), "valorCredito", credito, "clienteId", cliente))
                .statusCode(sucesso()).extract().jsonPath().getLong("id");
        CLIENTE_DA_COTA.put(cota, Pessoa.cliente(cliente));
        return cota;
    }

    public static long abrirProcesso(long cota, String tipo) {
        return abrirProcesso(cota, tipo, VALOR_PADRAO);
    }

    /** Abre com UM favorecido válido que recebe o valor inteiro (R15, R29). */
    public static long abrirProcesso(long cota, String tipo, BigDecimal valor) {
        return abrirProcesso(cota, tipo, valor, favorecidoUnico(valor));
    }

    public static long abrirProcesso(long cota, String tipo, BigDecimal valor, Object favorecidos) {
        return tentarAbrirProcesso(cota, tipo, valor, favorecidos).statusCode(201).extract().jsonPath().getLong("id");
    }

    /** Pedido de abertura sem conferir o resultado, com o valor padrão. */
    public static ValidatableResponse tentarAbrirProcesso(long cota, String tipo) {
        return tentarAbrirProcesso(cota, tipo, VALOR_PADRAO);
    }

    public static ValidatableResponse tentarAbrirProcesso(long cota, String tipo, BigDecimal valor) {
        return tentarAbrirProcesso(cota, tipo, valor, favorecidoUnico(valor));
    }

    /** {@code favorecidos}: lista de {@link #favorecido}, {@link #AUSENTE} (omite o campo) ou null (JSON null). */
    public static ValidatableResponse tentarAbrirProcesso(long cota, String tipo, BigDecimal valor, Object favorecidos) {
        return enviar("/processos", corpoAbertura(cota, tipo, valor, favorecidos));
    }

    /** Pedido de abertura com a identidade dada (valor padrão), sem conferir o resultado. */
    public static ValidatableResponse tentarAbrirProcessoComo(String autorizacao, long cota, String tipo) {
        return enviarComo(autorizacao, "/processos",
                corpoAbertura(cota, tipo, VALOR_PADRAO, favorecidoUnico(VALOR_PADRAO)));
    }

    /** Marca um campo que NÃO deve ser enviado (diferente de null, que vai como JSON null). */
    public static final Object AUSENTE = new Object() {
        @Override
        public String toString() {
            return "<ausente>";
        }
    };

    /** Um favorecido do pedido; cada argumento pode ser valor, {@link #AUSENTE} ou null. */
    public static Map<String, Object> favorecido(Object nome, Object documento, Object papel, Object valor) {
        Map<String, Object> favorecido = new LinkedHashMap<>();
        favorecido.put("nome", nome);
        favorecido.put("documento", documento);
        favorecido.put("papel", papel);
        favorecido.put("valor", valor);
        return favorecido;
    }

    /** Lista com um favorecido válido, com CPF próprio, que recebe o valor inteiro. */
    public static List<Map<String, Object>> favorecidoUnico(BigDecimal valor) {
        return List.of(favorecido("Favorecido de Aceitacao", "%011d".formatted(unico()), "vendedor", valor));
    }

    /** Corpo do pedido de abertura como texto JSON (o teste controla o que vai, inclusive o que falta). */
    public static String corpoAbertura(Object cotaId, Object tipo, Object valor, Object favorecidos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("cotaId", cotaId);
        corpo.put("tipo", tipo);
        corpo.put("valor", valor);
        corpo.put("favorecidos", favorecidos);
        return json(corpo);
    }

    /** GET /processos/{id}/favorecidos sem conferir o resultado. */
    public static ValidatableResponse tentarConsultarFavorecidos(long processo) {
        return requisicaoDeConsulta().when().get("/processos/%d/favorecidos".formatted(processo)).then();
    }

    /** Favorecidos do processo, na ordem devolvida; {@code valor} fica como BigDecimal quando o JSON permite. */
    public static List<Map<String, Object>> consultarFavorecidos(long processo) {
        String corpo = tentarConsultarFavorecidos(processo).statusCode(200).extract().asString();
        return comoBigDecimal(corpo).getList("$");
    }

    /** Serializa Map, lista, texto, número, booleano e null. Texto fora do ASCII sai em escape unicode, sem depender de charset. */
    public static String json(Object valor) {
        StringBuilder texto = new StringBuilder();
        escrever(texto, valor);
        return texto.toString();
    }

    private static void escrever(StringBuilder saida, Object valor) {
        switch (valor) {
            case null -> saida.append("null");
            case BigDecimal decimal -> saida.append(decimal.toPlainString());
            case Number numero -> saida.append(numero);
            case Boolean booleano -> saida.append(booleano);
            case CharSequence texto -> escreverTexto(saida, texto);
            case Map<?, ?> mapa -> {
                saida.append('{');
                boolean primeiro = true;
                for (Map.Entry<?, ?> entrada : mapa.entrySet()) {
                    if (entrada.getValue() == AUSENTE) {
                        continue;
                    }
                    if (!primeiro) {
                        saida.append(',');
                    }
                    primeiro = false;
                    escreverTexto(saida, entrada.getKey().toString());
                    saida.append(':');
                    escrever(saida, entrada.getValue());
                }
                saida.append('}');
            }
            case Collection<?> colecao -> {
                saida.append('[');
                boolean primeiro = true;
                for (Object item : colecao) {
                    if (!primeiro) {
                        saida.append(',');
                    }
                    primeiro = false;
                    escrever(saida, item);
                }
                saida.append(']');
            }
            default -> throw new IllegalArgumentException("Tipo sem JSON no teste: " + valor.getClass());
        }
    }

    private static void escreverTexto(StringBuilder saida, CharSequence texto) {
        saida.append('"');
        for (int i = 0; i < texto.length(); i++) {
            char c = texto.charAt(i);
            if (c == '"' || c == '\\') {
                saida.append('\\').append(c);
            } else if (c < 0x20 || c > 0x7e) {
                saida.append("\\u%04x".formatted((int) c));
            } else {
                saida.append(c);
            }
        }
        saida.append('"');
    }

    /** Crédito disponível informado pela consulta da cota (R13), lido sem perder casas decimais. */
    public static BigDecimal consultarCreditoDisponivel(long cota) {
        String corpo = requisicaoDeConsulta().when().get("/cotas/%d".formatted(cota)).then().statusCode(200)
                .extract().asString();
        Object valor = comoBigDecimal(corpo).get("creditoDisponivel");
        if (valor == null) {
            throw new AssertionError("A consulta da cota %d não informa creditoDisponivel: %s".formatted(cota, corpo));
        }
        return new BigDecimal(valor.toString());
    }

    /** Valor de um item da listagem de processos. */
    public static BigDecimal valorDoProcesso(Map<String, Object> item) {
        Object valor = item.get("valor");
        if (valor == null) {
            throw new AssertionError("O item da listagem não informa valor: " + item);
        }
        return new BigDecimal(valor.toString());
    }

    /**
     * POST com o token padrão do caminho: abrir processo, o cliente dono da cota citada no corpo; cadastros de
     * cliente e de cota, o backoffice.
     */
    public static ValidatableResponse enviar(String caminho, Object corpo) {
        String autorizacao = "/processos".equals(caminho) ? autorizacaoPadraoDaAbertura(corpo)
                : IdentidadeTeste.ANA.autorizacao();
        return enviarComo(autorizacao, caminho, corpo);
    }

    /** POST com a identidade dada. Um processo aberto (201) fica conhecido, para o auxiliar achar o dono da cota. */
    public static ValidatableResponse enviarComo(String autorizacao, String caminho, Object corpo) {
        ValidatableResponse resposta = requisicao(autorizacao).contentType(ContentType.JSON).body(corpo)
                .when().post(caminho).then();
        if ("/processos".equals(caminho) && resposta.extract().statusCode() == 201) {
            Long cota = cotaDoCorpo(corpo);
            if (cota != null) {
                COTA_DO_PROCESSO.put(resposta.extract().jsonPath().getLong("id"), cota);
            }
        }
        return resposta;
    }

    /** GET com a identidade dada. */
    public static ValidatableResponse consultarComo(String autorizacao, String caminho) {
        return requisicao(autorizacao).when().get(caminho).then();
    }

    public static ValidatableResponse executar(long processo, String acao) {
        return executarComo(autorizacaoPadraoDaAcao(processo, acao), processo, acao, null);
    }

    /** A ação com a identidade dada; {@code corpo} nulo envia o pedido sem corpo (objeto ou texto JSON, se houver). */
    public static ValidatableResponse executarComo(String autorizacao, long processo, String acao, Object corpo) {
        RequestSpecification pedido = requisicao(autorizacao);
        if (corpo != null) {
            pedido = pedido.contentType(ContentType.JSON).body(corpo);
        }
        return pedido.when().post("/processos/%d/%s".formatted(processo, acao)).then();
    }

    /** Ação cujo corpo é obrigatório e traz o motivo: {"motivo": "..."} . */
    public static ValidatableResponse executarComMotivo(long processo, String acao, String motivo) {
        return executarComCorpo(processo, acao, Map.of("motivo", motivo));
    }

    /** Ação cujo corpo é opcional: {"observacao": "..."} . */
    public static ValidatableResponse executarComObservacao(long processo, String acao, String observacao) {
        return executarComCorpo(processo, acao, Map.of("observacao", observacao));
    }

    /** Corpo JSON como objeto (serializado) ou como texto cru, para poder enviar JSON malformado. */
    public static ValidatableResponse executarComCorpo(long processo, String acao, Object corpo) {
        return executarComo(autorizacaoPadraoDaAcao(processo, acao), processo, acao, corpo);
    }

    /** Corpo com conteúdo e tipo escolhido pelo teste (ex.: text/plain). */
    public static ValidatableResponse executarComConteudo(long processo, String acao, String tipo, String corpo) {
        return requisicao(autorizacaoPadraoDaAcao(processo, acao)).contentType(tipo).body(corpo)
                .when().post("/processos/%d/%s".formatted(processo, acao)).then();
    }

    public static List<String> consultarAcoes(long processo) {
        return requisicaoDeConsulta().when().get("/processos/%d/acoes".formatted(processo)).then().statusCode(200)
                .extract().jsonPath().getList("acoes");
    }

    /** Histórico em ordem; os números (valorAnterior, valorNovo) ficam como BigDecimal, sem ponto flutuante. */
    public static List<Map<String, Object>> consultarHistorico(long processo) {
        String corpo = requisicaoDeConsulta().when().get("/processos/%d/historico".formatted(processo)).then()
                .statusCode(200)
                .extract().asString();
        return comoBigDecimal(corpo).getList("$");
    }

    // ---- Fatia 5: pagamento como saga (R7, R20 a R22, R31) ----

    /** Ação do histórico para cada tentativa da rotina sem resultado aplicado (indisponível, sem resposta, incoerente). */
    public static final String TENTAR_PAGAMENTO = "TENTAR_PAGAMENTO";

    /** Tempo máximo que um teste espera pelo efeito da rotina, que roda sozinha, por tempo. */
    private static final Duration ESPERA_MAXIMA = Duration.ofSeconds(30);

    private static final Duration INTERVALO_DA_CONSULTA = Duration.ofMillis(100);

    /**
     * Consulta em laço até a condição valer, ou falha depois de {@link #ESPERA_MAXIMA} dizendo o que faltou e o
     * último valor visto. A espera é só pela consulta: nenhuma asserção depende de um tempo fixo.
     */
    public static <T> T aguardar(String descricao, Supplier<T> consulta, Predicate<? super T> condicao) {
        long limite = System.nanoTime() + ESPERA_MAXIMA.toNanos();
        T ultimo = consulta.get();
        while (!condicao.test(ultimo)) {
            if (System.nanoTime() > limite) {
                throw new AssertionError("Passaram %d segundos sem que %s. Último valor visto: %s"
                        .formatted(ESPERA_MAXIMA.toSeconds(), descricao, ultimo));
            }
            try {
                Thread.sleep(INTERVALO_DA_CONSULTA.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Espera interrompida: " + descricao, e);
            }
            ultimo = consulta.get();
        }
        return ultimo;
    }

    /**
     * Espera o processo chegar à situação, olhando a última entrada do histórico (consulta barata por processo,
     * que também acompanha as entradas da rotina). Processo sem histórico está aberto.
     */
    public static void aguardarSituacao(long processo, String situacao) {
        aguardar("o processo %d chegasse a %s".formatted(processo, situacao),
                () -> situacaoPeloHistorico(processo), situacao::equals);
    }

    private static String situacaoPeloHistorico(long processo) {
        List<Map<String, Object>> historico = consultarHistorico(processo);
        return historico.isEmpty() ? "ABERTO" : (String) historico.getLast().get("para");
    }

    /** Histórico sem as entradas de tentativa da rotina, que se repetem a cada intervalo e não têm número certo. */
    public static List<Map<String, Object>> consultarHistoricoSemTentativas(long processo) {
        return consultarHistorico(processo).stream()
                .filter(item -> !TENTAR_PAGAMENTO.equals(item.get("acao"))).toList();
    }

    public static long quantidadeDeTentativas(long processo) {
        return consultarHistorico(processo).stream().filter(item -> TENTAR_PAGAMENTO.equals(item.get("acao"))).count();
    }

    /** Espera a rotina registrar ao menos {@code minimo} tentativas sem resultado aplicado. */
    public static void aguardarTentativas(long processo, int minimo) {
        aguardar("o histórico do processo %d tivesse %d tentativas da rotina".formatted(processo, minimo),
                () -> quantidadeDeTentativas(processo), quantidade -> quantidade >= minimo);
    }

    // ---- Fatia 4: alteração de valor e favorecidos (R24 a R28) ----

    /** Corpo de {@code alterar}: {@code valor} e {@code favorecidos} podem ser valor, {@link #AUSENTE} ou null. */
    public static String corpoAlteracao(Object valor, Object favorecidos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("valor", valor);
        corpo.put("favorecidos", favorecidos);
        return json(corpo);
    }

    /** Corpo de {@code propor-alteracao}: como o de alterar, mais o motivo (valor, {@link #AUSENTE} ou null). */
    public static String corpoProposta(Object valor, Object favorecidos, Object motivo) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("valor", valor);
        corpo.put("favorecidos", favorecidos);
        corpo.put("motivo", motivo);
        return json(corpo);
    }

    /** POST /processos/{id}/alterar (cliente, processo aberto) sem conferir o resultado. */
    public static ValidatableResponse tentarAlterar(long processo, Object valor, Object favorecidos) {
        return executarComCorpo(processo, "alterar", corpoAlteracao(valor, favorecidos));
    }

    /** POST /processos/{id}/propor-alteracao (backoffice, processo em análise) sem conferir o resultado. */
    public static ValidatableResponse tentarProporAlteracao(long processo, Object valor, Object favorecidos,
            Object motivo) {
        return executarComCorpo(processo, "propor-alteracao", corpoProposta(valor, favorecidos, motivo));
    }

    /** Alteração proposta, como a consulta a devolve. */
    public record Proposta(BigDecimal valor, List<Map<String, Object>> favorecidos, String motivo) {
    }

    /** GET /processos/{id}/alteracao-proposta sem conferir o resultado. */
    public static ValidatableResponse tentarConsultarProposta(long processo) {
        return requisicaoDeConsulta().when().get("/processos/%d/alteracao-proposta".formatted(processo)).then();
    }

    /** Proposta pendente (200); números lidos sem perder casas decimais. */
    public static Proposta consultarProposta(long processo) {
        String corpo = tentarConsultarProposta(processo).statusCode(200).extract().asString();
        JsonPath resposta = comoBigDecimal(corpo);
        Object valor = resposta.get("valor");
        if (valor == null) {
            throw new AssertionError("A proposta do processo %d não informa valor: %s".formatted(processo, corpo));
        }
        List<Map<String, Object>> favorecidos = resposta.getList("favorecidos");
        return new Proposta(new BigDecimal(valor.toString()), favorecidos, resposta.getString("motivo"));
    }

    /** Todos os processos da listagem filtrada por situação, percorrendo as páginas. */
    public static List<Map<String, Object>> listarProcessosPorSituacao(String situacao) {
        List<Map<String, Object>> todos = new ArrayList<>();
        for (int pagina = 1;; pagina++) {
            List<Map<String, Object>> itens = requisicaoDeConsulta().queryParam("estado", situacao)
                    .queryParam("pagina", pagina).queryParam("tamanho", 100)
                    .when().get("/processos").then().statusCode(200).extract().jsonPath().getList("itens");
            if (itens.isEmpty()) {
                return todos;
            }
            todos.addAll(itens);
        }
    }

    /** O contrato não tem consulta de processo por id: procura na listagem completa. */
    public static Map<String, Object> consultarProcesso(long processo) {
        return listarProcessos().stream()
                .filter(item -> numero(item, "id") == processo)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Processo %d não aparece na listagem".formatted(processo)));
    }

    public static long quantidadeDeProcessosDaCota(long cota) {
        return listarProcessos().stream().filter(item -> numero(item, "cotaId") == cota).count();
    }

    private static List<Map<String, Object>> listarProcessos() {
        List<Map<String, Object>> todos = new ArrayList<>();
        for (int pagina = 1;; pagina++) {
            String corpo = requisicaoDeConsulta().queryParam("pagina", pagina).queryParam("tamanho", 100)
                    .when().get("/processos").then().statusCode(200).extract().asString();
            JsonPath resposta = comoBigDecimal(corpo);
            List<Map<String, Object>> itens = resposta.getList("itens");
            if (itens.isEmpty()) {
                return todos;
            }
            todos.addAll(itens);
        }
    }

    /** Números JSON lidos como BigDecimal: dinheiro não passa por ponto flutuante. */
    private static JsonPath comoBigDecimal(String corpo) {
        return new JsonPath(corpo).using(JsonPathConfig.jsonPathConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));
    }

    private static long numero(Map<String, Object> item, String campo) {
        return ((Number) item.get(campo)).longValue();
    }

    private static long unico() {
        return SEQUENCIA.incrementAndGet();
    }
}
