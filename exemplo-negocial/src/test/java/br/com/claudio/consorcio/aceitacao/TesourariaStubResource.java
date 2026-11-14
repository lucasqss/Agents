package br.com.claudio.consorcio.aceitacao;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.HashMap;
import java.util.Map;

/**
 * Padrão: Service Stub, ciclo de vida [PoEAA].
 * Sobe a tesouraria simulada no JVM do teste. Único ponto, com {@link TesourariaStub}, que conhece a integração.
 *
 * <p>Entrega também a configuração de identidade da aplicação de teste (fatia 7, R18): a chave pública de teste em que
 * ela confia para os tokens e o emissor esperado, de {@link IdentidadeTeste}. Fica neste recurso para valer, com um só
 * ciclo de vida, no {@code *Test} e no {@code *IT}.
 */
public class TesourariaStubResource implements QuarkusTestResourceLifecycleManager {

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        System.setProperty(TesourariaStub.PROPRIEDADE_PORTA, String.valueOf(servidor.port()));
        // A rotina de pagamento roda sozinha, por tempo: no ambiente de teste, a cada segundo (em vez dos 5 minutos).
        Map<String, String> configuracao = new HashMap<>();
        configuracao.put("quarkus.rest-client.tesouraria.url", servidor.baseUrl());
        configuracao.put("pagamento.rotina.intervalo", "1s");
        configuracao.putAll(IdentidadeTeste.configuracaoDaAplicacao());
        return configuracao;
    }

    @Override
    public void stop() {
        if (servidor != null) {
            servidor.stop();
        }
    }
}
