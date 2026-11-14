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

    /**
     * Chave por método da configuração do SmallRye Fault Tolerance: {@code "<classe>/<método>"}. Num REST client de
     * interface a classe que vale é a do wrapper CDI que o Quarkus gera ({@code TesourariaClient$$CDIWrapper}), não a
     * da interface onde estão as anotações.
     */
    private static final String METODO_DO_CLIENTE =
            "\"br.com.claudio.consorcio.gateway.tesouraria.TesourariaClient$$CDIWrapper/criarOrdem\"";

    private WireMockServer servidor;

    @Override
    public Map<String, String> start() {
        servidor = new WireMockServer(options().dynamicPort());
        servidor.start();
        System.setProperty(TesourariaStub.PROPRIEDADE_PORTA, String.valueOf(servidor.port()));
        // A rotina de pagamento roda sozinha, por tempo: no ambiente de teste, a cada 100 ms (em vez dos 5 minutos).
        Map<String, String> configuracao = new HashMap<>();
        configuracao.put("quarkus.rest-client.tesouraria.url", servidor.baseUrl());
        configuracao.put("pagamento.rotina.intervalo", "100ms");
        // Resiliência do TesourariaClient encurtada só nos testes (as anotações, 2 s e 5 s, seguem como estão).
        configuracao.put("quarkus.fault-tolerance." + METODO_DO_CLIENTE + ".timeout.value",
                String.valueOf(TesourariaStub.TEMPO_PARA_RESPONDER.toMillis()));
        configuracao.put("quarkus.fault-tolerance." + METODO_DO_CLIENTE + ".circuit-breaker.delay",
                String.valueOf(TesourariaStub.ESPERA_DO_CIRCUITO.toMillis()));
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
