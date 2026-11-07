package br.com.claudio.consorcio.shared.gateway.tesouraria.client;

import br.com.claudio.consorcio.shared.gateway.tesouraria.dto.OrdemPagamentoRequest;
import br.com.claudio.consorcio.shared.gateway.tesouraria.dto.OrdemPagamentoResponse;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import org.eclipse.microprofile.faulttolerance.Retry;
import org.eclipse.microprofile.faulttolerance.Timeout;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente HTTP da tesouraria (URL em quarkus.rest-client.tesouraria.url). A resiliência fica aqui:
 * cada tentativa tem timeout, falhas são repetidas e, se o sistema seguir falhando, o circuito abre.
 */
@RegisterRestClient(configKey = "tesouraria")
@Path("/ordens-pagamento")
public interface TesourariaClient {

    @POST
    @Timeout(2000)
    @Retry(maxRetries = 2, delay = 200)
    @CircuitBreaker(requestVolumeThreshold = 10, failureRatio = 0.5, delay = 5000)
    OrdemPagamentoResponse criarOrdem(OrdemPagamentoRequest request);
}
