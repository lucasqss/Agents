package br.com.claudio.consorcio.shared.integracao.tesouraria;

import br.com.claudio.consorcio.shared.integracao.IntegracaoIndisponivelException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Gateway (PoEAA) da tesouraria: a única porta de entrada para esse sistema. Expõe uma API simples,
 * no nosso vocabulário, traduz o modelo externo e converte qualquer falha em
 * {@link IntegracaoIndisponivelException}.
 */
@ApplicationScoped
public class TesourariaGateway {

    static final String SISTEMA = "Tesouraria";

    private final TesourariaClient client;

    public TesourariaGateway(@RestClient TesourariaClient client) {
        this.client = client;
    }

    /** Registra a ordem de pagamento do processo e devolve o protocolo emitido pela tesouraria. */
    public String registrarPagamento(Long processoId, Long cotaId) {
        OrdemPagamentoResponse resposta;
        try {
            resposta = client.criarOrdem(new OrdemPagamentoRequest("CONSORCIO-PROCESSO-" + processoId, cotaId));
        } catch (RuntimeException e) {
            throw new IntegracaoIndisponivelException(SISTEMA, e);
        }
        if (resposta == null || resposta.protocolNumber() == null || resposta.protocolNumber().isBlank()) {
            throw new IntegracaoIndisponivelException(SISTEMA,
                    new IllegalStateException("Resposta sem protocolo: " + resposta));
        }
        return resposta.protocolNumber();
    }
}
