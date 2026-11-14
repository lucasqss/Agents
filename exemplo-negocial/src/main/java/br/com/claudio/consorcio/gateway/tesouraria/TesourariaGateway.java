package br.com.claudio.consorcio.gateway.tesouraria;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.eclipse.microprofile.rest.client.inject.RestClient;

/**
 * Gateway (PoEAA) da tesouraria: a única porta de entrada para esse sistema. Expõe uma API simples, no nosso
 * vocabulário, e traduz tudo para {@link RespostaDaTesouraria}, inclusive 5xx, timeout e circuito aberto: quem chama
 * (a rotina de pagamento) não recebe exceção. Tentar de novo é da rotina.
 */
@ApplicationScoped
public class TesourariaGateway {

    static final String SISTEMA = "Tesouraria";

    private static final int TAMANHO_MAXIMO_DA_DESCRICAO = 200;

    private final TesourariaClient client;

    public TesourariaGateway(@RestClient TesourariaClient client) {
        this.client = client;
    }

    /**
     * Pede o pagamento. A identificação do pedido é a chave de idempotência: repetida, a tesouraria devolve o resultado
     * da ordem que já abriu (ALREADY_PROCESSED) em vez de abrir outra.
     */
    public RespostaDaTesouraria solicitarPagamento(PedidoDePagamento pedido) {
        OrdemPagamentoResponse resposta;
        try {
            resposta = client.criarOrdem(paraOrdem(pedido));
        } catch (RuntimeException e) {
            return new RespostaDaTesouraria.SemResposta(SISTEMA + " sem resposta: " + descrever(e));
        }
        return traduzir(resposta);
    }

    private static RespostaDaTesouraria traduzir(OrdemPagamentoResponse resposta) {
        if (resposta == null || resposta.status() == null) {
            return new RespostaDaTesouraria.Incoerente(SISTEMA + " respondeu sem status");
        }
        return switch (resposta.status()) {
            case "ACCEPTED", "ALREADY_PROCESSED" -> new RespostaDaTesouraria.Confirmada(resposta.protocolNumber());
            case "REJECTED" -> new RespostaDaTesouraria.Recusada(resposta.reason());
            default -> new RespostaDaTesouraria.Incoerente(SISTEMA + " respondeu com status desconhecido: "
                    + limitar(resposta.status()));
        };
    }

    private static OrdemPagamentoRequest paraOrdem(PedidoDePagamento pedido) {
        List<OrdemPagamentoRequest.Payee> favorecidos = pedido.partes().stream()
                .map(p -> new OrdemPagamentoRequest.Payee(p.nome(), p.documento(), p.papel(), p.valor()))
                .toList();
        return new OrdemPagamentoRequest(pedido.identificacao(), pedido.processoId(), pedido.valor(), favorecidos);
    }

    private static String descrever(RuntimeException e) {
        String mensagem = e.getMessage() == null ? "" : ": " + e.getMessage();
        return limitar(e.getClass().getSimpleName() + mensagem);
    }

    private static String limitar(String texto) {
        return texto.length() <= TAMANHO_MAXIMO_DA_DESCRICAO ? texto : texto.substring(0, TAMANHO_MAXIMO_DA_DESCRICAO);
    }
}
