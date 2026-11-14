package br.com.claudio.consorcio.aceitacao;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.json;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;
import io.restassured.path.json.JsonPath;
import io.restassured.path.json.config.JsonPathConfig;
import io.restassured.path.json.config.JsonPathConfig.NumberReturnType;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Padrão: Service Stub, controle [PoEAA].
 * Os cenários falam em negócio; o contrato da tesouraria fica só aqui. O controle é por HTTP porque, no *IT,
 * a aplicação roda em outro processo.
 *
 * <p>Contrato da tesouraria usado (decidido por quem coordena; ver "Contrato usado" no caso de uso):
 * {@code POST /ordens-pagamento} com {@code {"requestId", "processId", "amount", "payees": [{"name", "document",
 * "role", "amount"}]}}; resposta 200 com {@code {"status": "ACCEPTED", "protocolNumber"}},
 * {@code {"status": "REJECTED", "reason"}} ou {@code {"status": "ALREADY_PROCESSED", "protocolNumber"}}; 5xx ou
 * demora acima de 2 segundos é indisponível ou sem resposta.
 *
 * <p>O estado do stub é compartilhado: a rotina da aplicação roda o tempo todo sobre todos os processos em
 * pagamento solicitado. Quem muda o comportamento o devolve a {@link #confirmar()} ao terminar, e as consultas ao
 * que a tesouraria recebeu filtram sempre pelo processo.
 */
public final class TesourariaStub {

    /** Protocolo que só este stub emite: encontrá-lo no processo prova que a chamada passou por aqui. */
    public static final String PROTOCOLO = "PROTOCOLO-STUB-ACEITACAO";

    /** Tempo de resposta que o stub usa para "não respondeu": acima dos 2 segundos que a tesouraria tem para responder. */
    public static final Duration DEMORA_SEM_RESPOSTA = Duration.ofSeconds(3);

    static final String PROPRIEDADE_PORTA = "aceitacao.tesouraria.porta";

    private static final String CAMINHO_ORDENS = "/ordens-pagamento";

    /** Prioridade das respostas específicas de um processo: valem antes da resposta geral (prioridade padrão, 5). */
    private static final int PRIORIDADE_DO_PROCESSO = 1;

    /** Pedido que a tesouraria recebeu, no contrato dela. */
    public record OrdemRecebida(String requestId, long processId, BigDecimal valor, List<Parte> partes) {
    }

    /** Parte de um favorecido no pedido que a tesouraria recebeu. */
    public record Parte(String nome, String documento, String papel, BigDecimal valor) {
    }

    private TesourariaStub() {
    }

    // ---- Comportamento geral da tesouraria ----

    /** Confirma com o protocolo padrão do stub. */
    public static void confirmar() {
        confirmar(PROTOCOLO);
    }

    public static void confirmar(String protocolo) {
        responderATodos(resposta(Map.of("status", "ACCEPTED", "protocolNumber", protocolo)));
    }

    /** Confirma depois de {@code atraso}, ainda dentro do tempo que a tesouraria tem para responder. */
    public static void confirmarComAtraso(Duration atraso, String protocolo) {
        responderATodos(resposta(Map.of("status", "ACCEPTED", "protocolNumber", protocolo))
                .withFixedDelay((int) atraso.toMillis()));
    }

    public static void recusar(String motivo) {
        responderATodos(resposta(Map.of("status", "REJECTED", "reason", motivo)));
    }

    /** "Já fiz essa ordem": a tesouraria informa o protocolo de uma ordem anterior da mesma solicitação. */
    public static void jaProcessou(String protocolo) {
        responderATodos(resposta(Map.of("status", "ALREADY_PROCESSED", "protocolNumber", protocolo)));
    }

    public static void ficarIndisponivel() {
        responderATodos(aResponse().withStatus(503));
    }

    /** Responde (com confirmação) só depois de {@link #DEMORA_SEM_RESPOSTA}: para a aplicação, não respondeu. */
    public static void demorar() {
        responderATodos(resposta(Map.of("status", "ACCEPTED", "protocolNumber", PROTOCOLO))
                .withFixedDelay((int) DEMORA_SEM_RESPOSTA.toMillis()));
    }

    /** Resposta incoerente: confirma, mas sem protocolo. */
    public static void confirmarSemProtocolo() {
        responderATodos(resposta(Map.of("status", "ACCEPTED")));
    }

    /** Resposta incoerente: recusa, mas sem motivo. */
    public static void recusarSemMotivo() {
        responderATodos(resposta(Map.of("status", "REJECTED")));
    }

    // ---- Comportamento para um processo só (vale antes do geral até o próximo comportamento geral) ----

    public static void confirmarProcesso(long processo, String protocolo) {
        cliente().register(post(urlEqualTo(CAMINHO_ORDENS)).atPriority(PRIORIDADE_DO_PROCESSO)
                .withRequestBody(matching(doProcesso(processo)))
                .willReturn(resposta(Map.of("status", "ACCEPTED", "protocolNumber", protocolo))));
    }

    public static void recusarProcesso(long processo, String motivo) {
        cliente().register(post(urlEqualTo(CAMINHO_ORDENS)).atPriority(PRIORIDADE_DO_PROCESSO)
                .withRequestBody(matching(doProcesso(processo)))
                .willReturn(resposta(Map.of("status", "REJECTED", "reason", motivo))));
    }

    // ---- O que a tesouraria RECEBEU (journal do WireMock), sempre de um processo ----

    /** Pedidos que a tesouraria recebeu para o processo, do mais antigo ao mais novo. */
    public static List<OrdemRecebida> ordensRecebidas(long processo) {
        List<LoggedRequest> pedidos = new ArrayList<>(cliente().find(
                postRequestedFor(urlEqualTo(CAMINHO_ORDENS)).withRequestBody(matching(doProcesso(processo)))));
        pedidos.sort(Comparator.comparing(LoggedRequest::getLoggedDate));
        List<OrdemRecebida> ordens = new ArrayList<>();
        for (LoggedRequest pedido : pedidos) {
            OrdemRecebida ordem = ler(pedido.getBodyAsString());
            if (ordem.processId() == processo) {
                ordens.add(ordem);
            }
        }
        return ordens;
    }

    public static int quantidadeDeOrdens(long processo) {
        return ordensRecebidas(processo).size();
    }

    /** Identificações de solicitação vistas nos pedidos do processo, sem repetição, na ordem em que apareceram. */
    public static List<String> identificacoesRecebidas(long processo) {
        return ordensRecebidas(processo).stream().map(OrdemRecebida::requestId).distinct().toList();
    }

    private static OrdemRecebida ler(String corpo) {
        JsonPath resposta = new JsonPath(corpo)
                .using(JsonPathConfig.jsonPathConfig().numberReturnType(NumberReturnType.BIG_DECIMAL));
        List<Parte> partes = new ArrayList<>();
        List<Map<String, Object>> favorecidos = resposta.getList("payees");
        if (favorecidos != null) {
            for (Map<String, Object> favorecido : favorecidos) {
                partes.add(new Parte((String) favorecido.get("name"), (String) favorecido.get("document"),
                        (String) favorecido.get("role"), decimal(favorecido.get("amount"))));
            }
        }
        Object processo = resposta.get("processId");
        return new OrdemRecebida(resposta.getString("requestId"),
                processo == null ? -1 : new BigDecimal(processo.toString()).longValueExact(),
                decimal(resposta.get("amount")), partes);
    }

    private static BigDecimal decimal(Object valor) {
        return valor == null ? null : new BigDecimal(valor.toString());
    }

    // ---- Mecânica ----

    private static String doProcesso(long processo) {
        return "(?s).*\"processId\"\\s*:\\s*%d\\s*[,}].*".formatted(processo);
    }

    private static ResponseDefinitionBuilder resposta(Map<String, Object> corpo) {
        return aResponse().withStatus(200).withHeader("Content-Type", "application/json")
                .withBody(json(new LinkedHashMap<>(corpo)));
    }

    private static void responderATodos(ResponseDefinitionBuilder resposta) {
        WireMock cliente = cliente();
        // Não usar resetMappings(): no WireMock 3 ele também apaga o journal, que os casos consultam depois.
        cliente.allStubMappings().getMappings().forEach(cliente::removeStubMapping);
        MappingBuilder mapeamento = post(urlEqualTo(CAMINHO_ORDENS)).willReturn(resposta);
        cliente.register(mapeamento);
    }

    private static WireMock cliente() {
        return new WireMock("localhost", Integer.getInteger(PROPRIEDADE_PORTA));
    }
}
