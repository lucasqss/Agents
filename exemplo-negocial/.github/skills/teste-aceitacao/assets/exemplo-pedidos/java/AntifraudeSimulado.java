package PACOTE_RAIZ.aceitacao;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/**
 * Antifraude simulado, respondendo a partir de massa/externa-antifraude.csv. Sobe antes da aplicação, no *Test e no
 * *IT, e entrega a ela a URL do simulador. Uma linha da massa vira uma resposta: o resto do teste não sabe que existe.
 */
public class AntifraudeSimulado implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        for (Map<String, String> linha : Massa.ler("aceitacao/pedidos/massa/externa-antifraude.csv")) {
            servidor.stubFor(get(urlPathEqualTo("/avaliacoes/" + linha.get("documento")))
                    .willReturn(aResponse().withHeader("Content-Type", "application/json")
                            .withBody("{\"aprovado\": " + "sim".equals(linha.get("aprovado")) + "}")));
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
