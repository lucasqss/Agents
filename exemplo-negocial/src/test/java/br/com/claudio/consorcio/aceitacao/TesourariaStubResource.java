package br.com.claudio.consorcio.aceitacao;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;

/**
 * Padrão: Service Stub, ciclo de vida [PoEAA].
 * Sobe a tesouraria simulada no JVM do teste. Único ponto, com {@link TesourariaStub}, que conhece a integração.
 */
public class TesourariaStubResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        System.setProperty(TesourariaStub.PROPRIEDADE_PORTA, String.valueOf(servidor.port()));
        return Map.of("quarkus.rest-client.tesouraria.url", servidor.baseUrl());
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
