package br.com.claudio.consorcio.canal.web;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service Stub (PoEAA) da tesouraria: um recurso servido pela própria app nos testes, para onde o
 * REST client aponta (%test.quarkus.rest-client.tesouraria.url). Exercita o client, o JSON e a
 * tradução do Gateway de verdade, sem depender do sistema real.
 */
@Path("/fake-tesouraria/ordens-pagamento")
public class TesourariaFake {

    static volatile boolean falhar;

    private static final AtomicLong PROTOCOLOS = new AtomicLong();

    @POST
    public Map<String, Object> criarOrdem(Map<String, Object> ordem) {
        if (falhar) {
            throw new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE);
        }
        return Map.of("protocolNumber", "TES-" + PROTOCOLOS.incrementAndGet(), "status", "ACCEPTED",
                "echo", ordem.get("externalReference"));
    }
}
