package br.com.claudio.consorcio.gateway.tesouraria;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente HTTP da tesouraria (URL em quarkus.rest-client.tesouraria.url). Cada chamada tem 2 segundos para responder.
 * Não há {@code @Retry}: quem repete é a rotina de pagamento, no próximo intervalo e com a mesma identificação.
 * O circuito abre depois de falhas seguidas e responde na hora, sem esperar o timeout, enquanto a tesouraria está
 * fora: a rodada da rotina não demora (2 s por processo) quando há muitos processos esperando.
 */
@RegisterRestClient(configKey = "tesouraria")
@Path("/ordens-pagamento")
public interface TesourariaClient {

    @POST
    @Timeout(2000)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    OrdemPagamentoResponse criarOrdem(OrdemPagamentoRequest request);
}
