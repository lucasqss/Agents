package br.com.claudio.consorcio.aceitacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.WireMock;

/**
 * Padrão: Service Stub, controle [PoEAA].
 * Os cenários falam em negócio; o contrato da tesouraria fica só aqui. O controle é por HTTP porque, no *IT,
 * a aplicação roda em outro processo.
 */
public final class TesourariaStub {

    /** Protocolo que só este stub emite: encontrá-lo no processo prova que a chamada passou por aqui. */
    public static final String PROTOCOLO = "PROTOCOLO-STUB-ACEITACAO";

    static final String PROPRIEDADE_PORTA = "aceitacao.tesouraria.porta";

    private static final String CAMINHO_ORDENS = "/ordens-pagamento";

    private TesourariaStub() {
    }

    public static void ficarDisponivel() {
        WireMock cliente = cliente();
        cliente.resetMappings();
        cliente.register(post(urlEqualTo(CAMINHO_ORDENS)).willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"protocolNumber\":\"%s\",\"status\":\"ACCEPTED\"}".formatted(PROTOCOLO))));
    }

    public static void ficarIndisponivel() {
        WireMock cliente = cliente();
        cliente.resetMappings();
        cliente.register(post(urlEqualTo(CAMINHO_ORDENS)).willReturn(aResponse().withStatus(503)));
    }

    private static WireMock cliente() {
        return new WireMock("localhost", Integer.getInteger(PROPRIEDADE_PORTA));
    }
}
