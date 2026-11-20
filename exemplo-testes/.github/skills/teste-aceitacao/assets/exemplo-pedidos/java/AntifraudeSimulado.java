package PACOTE_RAIZ.aceitacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/**
 * Antifraude simulado, respondendo a partir de massa/externa-antifraude.yaml. Sobe antes da aplicação, no *Test e no
 * *IT, e entrega a ela a URL do simulador. Um registro da massa vira uma resposta: o resto do teste não sabe que existe.
 */
public class AntifraudeSimulado implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        for (Map<String, Object> registro : Massa.ler("aceitacao/pedidos/massa/externa-antifraude.yaml")) {
            servidor.stubFor(get(urlPathEqualTo("/avaliacoes/" + registro.get("documento")))
                    .willReturn(aResponse().withHeader("Content-Type", "application/json")
                            .withBody("{\"aprovado\": " + registro.get("aprovado") + "}")));
        }
        return Map.of("quarkus.rest-client.antifraude.url", servidor.baseUrl());
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
