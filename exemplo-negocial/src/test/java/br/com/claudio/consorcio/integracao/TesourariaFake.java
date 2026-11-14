package br.com.claudio.consorcio.integracao;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service Stub (PoEAA) da tesouraria: um recurso servido pela própria app nos testes de integração, para onde o
 * REST client aponta (%test.quarkus.rest-client.tesouraria.url). Exercita o client, o JSON e a tradução do Gateway de
 * verdade, sem depender do sistema real. Fala o contrato da tesouraria ({@code requestId}, {@code processId},
 * {@code amount}, {@code payees}; {@code ACCEPTED}, {@code REJECTED}, {@code ALREADY_PROCESSED}) e é idempotente pelo
 * {@code requestId}, como a tesouraria de verdade: repetir o pedido devolve {@code ALREADY_PROCESSED} com o mesmo protocolo.
 * Os testes de aceitação usam outro stub (WireMock), que também roda no artefato empacotado.
 */
@Path("/fake-tesouraria/ordens-pagamento")
public class TesourariaFake {

    /** Como a tesouraria responde às próximas chamadas. */
    enum Modo {
        CONFIRMAR,
        RECUSAR,
        /** Responde 503. */
        INDISPONIVEL,
        /** Abre a ordem e cai antes de responder: a ordem existe lá, e o chamador recebe 500. */
        ABRIR_ORDEM_E_CAIR,
        CONFIRMAR_SEM_PROTOCOLO,
        RECUSAR_SEM_MOTIVO
    }

    static final String MOTIVO_DA_RECUSA = "conta inválida";

    private static final AtomicLong PROTOCOLOS = new AtomicLong();
    private static final Map<String, String> PROTOCOLO_POR_IDENTIFICACAO = new ConcurrentHashMap<>();
    private static final List<Pedido> RECEBIDOS = new CopyOnWriteArrayList<>();

    private static volatile Modo modo = Modo.CONFIRMAR;
    private static volatile long atrasoEmMilissegundos;

    /** Pedido que a tesouraria recebeu. */
    record Pedido(String requestId, long processId) {
    }

    static void responder(Modo novoModo) {
        modo = novoModo;
    }

    /** Faz a tesouraria demorar a responder, mas dentro do prazo, para as chamadas se sobreporem. */
    static void demorar(long milissegundos) {
        atrasoEmMilissegundos = milissegundos;
    }

    /** Volta ao comportamento normal: confirma, sem atraso. Não apaga o que já recebeu. */
    static void normalizar() {
        modo = Modo.CONFIRMAR;
        atrasoEmMilissegundos = 0;
    }

    /** Identificações (requestId) que a tesouraria recebeu para o processo, na ordem, uma por chamada. */
    static List<String> identificacoesRecebidas(long processoId) {
        return RECEBIDOS.stream().filter(p -> p.processId() == processoId).map(Pedido::requestId).toList();
    }

    /** Quantas ordens a tesouraria abriu para o processo (identificações diferentes que ela aceitou). */
    static long ordensAbertas(long processoId) {
        return identificacoesRecebidas(processoId).stream().distinct()
                .filter(PROTOCOLO_POR_IDENTIFICACAO::containsKey).count();
    }

    @POST
    public Map<String, Object> criarOrdem(Map<String, Object> ordem) throws InterruptedException {
        String requestId = String.valueOf(ordem.get("requestId"));
        RECEBIDOS.add(new Pedido(requestId, ((Number) ordem.get("processId")).longValue()));
        if (atrasoEmMilissegundos > 0) {
            Thread.sleep(atrasoEmMilissegundos);
        }
        return switch (modo) {
            case INDISPONIVEL -> throw new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE);
            case ABRIR_ORDEM_E_CAIR -> {
                PROTOCOLO_POR_IDENTIFICACAO.computeIfAbsent(requestId, id -> novoProtocolo());
                throw new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR);
            }
            case CONFIRMAR -> confirmar(requestId);
            case CONFIRMAR_SEM_PROTOCOLO -> resposta("ACCEPTED", null, null);
            case RECUSAR -> resposta("REJECTED", null, MOTIVO_DA_RECUSA);
            case RECUSAR_SEM_MOTIVO -> resposta("REJECTED", null, null);
        };
    }

    private static Map<String, Object> confirmar(String requestId) {
        boolean[] nova = {false};
        String protocolo = PROTOCOLO_POR_IDENTIFICACAO.computeIfAbsent(requestId, id -> {
            nova[0] = true;
            return novoProtocolo();
        });
        return resposta(nova[0] ? "ACCEPTED" : "ALREADY_PROCESSED", protocolo, null);
    }

    private static String novoProtocolo() {
        return "TES-" + PROTOCOLOS.incrementAndGet();
    }

    private static Map<String, Object> resposta(String status, String protocolo, String motivo) {
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("status", status);
        if (protocolo != null) {
            resposta.put("protocolNumber", protocolo);
        }
        if (motivo != null) {
            resposta.put("reason", motivo);
        }
        return resposta;
    }
}
