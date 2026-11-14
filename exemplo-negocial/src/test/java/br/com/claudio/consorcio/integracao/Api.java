package br.com.claudio.consorcio.integracao;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste;
import br.com.claudio.consorcio.aceitacao.IdentidadeTeste.Pessoa;
import io.restassured.RestAssured;
import io.restassured.filter.Filter;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Atalhos de RestAssured para os testes de integração. Desde a fatia 7 todo pedido leva um token JWT (ADR 003): o
 * {@link #given()} daqui acrescenta o do papel certo para o pedido, como {@code ApiAceitacao} faz nos testes de
 * aceitação: consultas, cadastros e a maioria das ações, o backoffice "ana"; aprovar na alçada, o aprovador "bruno"
 * (outra pessoa que a da conclusão da análise, R17); reprovar e devolver para análise, "carla" (backoffice e aprovador);
 * solicitar o pagamento, a tesouraria "tereza"; abrir, alterar, concordar e discordar, o cliente dono da cota.
 */
final class Api {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);

    /** Valor padrão do processo: válido (R12) e dentro do crédito de 250.000 das cotas dos testes (R13). */
    static final BigDecimal VALOR_PADRAO = new BigDecimal("30000.00");

    private static final Pessoa CLIENTE_SEM_COTA = Pessoa.cliente(987_654_321L);
    private static final Map<Long, Long> CLIENTE_DA_COTA = new ConcurrentHashMap<>();
    private static final Map<Long, Long> COTA_DO_PROCESSO = new ConcurrentHashMap<>();

    private Api() {
    }

    /** Pedido com o token do papel certo, salvo se o teste já pôs um cabeçalho {@code Authorization}. */
    static RequestSpecification given() {
        return RestAssured.given().filter(IDENTIDADE);
    }

    private static final Filter IDENTIDADE = (requisicao, resposta, contexto) -> {
        if (!requisicao.getHeaders().hasHeaderWithName("Authorization")) {
            requisicao.header("Authorization", quem(requisicao.getMethod(), URI.create(requisicao.getURI()).getPath(),
                    requisicao.getBody()).autorizacao());
        }
        Response recebida = contexto.next(requisicao, resposta);
        lembrarAbertura(requisicao.getMethod(), URI.create(requisicao.getURI()).getPath(), requisicao.getBody(), recebida);
        return recebida;
    };

    private static Pessoa quem(String metodo, String caminho, Object corpo) {
        if (!"POST".equals(metodo)) {
            return IdentidadeTeste.ANA;
        }
        if ("/processos".equals(caminho)) {
            return clienteDaCota(cotaDoCorpo(corpo));
        }
        String[] partes = caminho.split("/");
        if (partes.length != 4 || !"processos".equals(partes[1])) {
            return IdentidadeTeste.ANA;
        }
        return switch (partes[3]) {
            case "alterar", "concordar", "discordar" -> clienteDaCota(COTA_DO_PROCESSO.get(Long.valueOf(partes[2])));
            case "aprovar-na-alcada" -> IdentidadeTeste.BRUNO;
            case "reprovar", "devolver-para-analise" -> IdentidadeTeste.CARLA;
            case "solicitar-pagamento" -> IdentidadeTeste.TEREZA;
            default -> IdentidadeTeste.ANA;
        };
    }

    private static Pessoa clienteDaCota(Long cota) {
        Long cliente = cota == null ? null : CLIENTE_DA_COTA.get(cota);
        return cliente == null ? CLIENTE_SEM_COTA : Pessoa.cliente(cliente);
    }

    private static Long cotaDoCorpo(Object corpo) {
        try {
            Object cota = corpo instanceof Map<?, ?> mapa ? mapa.get("cotaId") : new JsonPath(String.valueOf(corpo)).get("cotaId");
            return cota instanceof Number numero ? numero.longValue() : null;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Guarda de que cota é cada processo aberto, para saber quem é o cliente dono nas ações que são dele. */
    private static void lembrarAbertura(String metodo, String caminho, Object corpo, Response resposta) {
        Long cota = "POST".equals(metodo) && "/processos".equals(caminho) && resposta.statusCode() == 201
                ? cotaDoCorpo(corpo) : null;
        if (cota != null) {
            COTA_DO_PROCESSO.put(resposta.jsonPath().getLong("id"), cota);
        }
    }

    static String unico(String prefixo) {
        return prefixo + SEQUENCIA.incrementAndGet();
    }

    static long criarCliente(String nome) {
        return post("/clientes", Map.of("nome", nome, "cpf", unico("CPF"))).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    static long criarCota(long clienteId) {
        long cota = post("/cotas", Map.of("numero", unico("COTA-"), "valorCredito", 250000, "clienteId", clienteId))
                .statusCode(201).extract().jsonPath().getLong("id");
        CLIENTE_DA_COTA.put(cota, clienteId);
        return cota;
    }

    static long criarProcesso(long cotaId, String tipo) {
        return post("/processos", corpoDeProcesso(cotaId, tipo)).statusCode(201)
                .extract().jsonPath().getLong("id");
    }

    /** Pedido de abertura válido: o valor padrão inteiro para um único favorecido com CPF próprio. */
    static Map<String, Object> corpoDeProcesso(long cotaId, String tipo) {
        return corpoDeProcesso(cotaId, tipo, VALOR_PADRAO,
                List.of(favorecido("Favorecido de Teste", "%011d".formatted(SEQUENCIA.incrementAndGet() % 100_000_000_000L),
                        "vendedor", VALOR_PADRAO)));
    }

    static Map<String, Object> corpoDeProcesso(long cotaId, String tipo, BigDecimal valor, List<Map<String, Object>> favorecidos) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("cotaId", cotaId);
        corpo.put("tipo", tipo);
        corpo.put("valor", valor);
        corpo.put("favorecidos", favorecidos);
        return corpo;
    }

    static Map<String, Object> favorecido(String nome, String documento, String papel, BigDecimal valor) {
        Map<String, Object> favorecido = new LinkedHashMap<>();
        favorecido.put("nome", nome);
        favorecido.put("documento", documento);
        favorecido.put("papel", papel);
        favorecido.put("valor", valor);
        return favorecido;
    }

    static ValidatableResponse acao(long processoId, String acao) {
        return given().when().post("/processos/" + processoId + "/" + acao).then();
    }

    /** Ação cujo corpo é obrigatório e traz o motivo. */
    static ValidatableResponse acaoComMotivo(long processoId, String acao, String motivo) {
        return post("/processos/" + processoId + "/" + acao, Map.of("motivo", motivo));
    }

    /** Solicita o pagamento: só grava Pagamento solicitado; a rotina é que fala com a tesouraria. */
    static ValidatableResponse solicitarPagamento(long processoId) {
        return acao(processoId, "solicitar-pagamento");
    }

    /** Estado do processo, lido da última entrada do histórico (ABERTO se ainda não há nenhuma). */
    static String estado(long processoId) {
        List<Map<String, Object>> historico = given().when().get("/processos/" + processoId + "/historico").then()
                .statusCode(200).extract().jsonPath().getList("$");
        return historico.isEmpty() ? "ABERTO" : (String) historico.getLast().get("para");
    }

    static List<Map<String, Object>> historico(long processoId) {
        return given().when().get("/processos/" + processoId + "/historico").then().statusCode(200)
                .extract().jsonPath().getList("$");
    }

    static ValidatableResponse post(String path, Object corpo) {
        return given().contentType(ContentType.JSON).body(corpo).when().post(path).then();
    }
}
