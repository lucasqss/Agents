package br.com.claudio.consorcio.gateway.tesouraria;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import jakarta.ws.rs.ServerErrorException;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;
import org.eclipse.microprofile.faulttolerance.exceptions.TimeoutException;
import org.junit.jupiter.api.Test;

/** O gateway traduz toda resposta da tesouraria, inclusive a falha, para {@link RespostaDaTesouraria}; nada vira exceção. */
class TesourariaGatewayTest {

    private static final PedidoDePagamento PEDIDO = new PedidoDePagamento("7c1f0c1e-5d7b-4a52-9d0c-3a3c9a3f3b11", 42L,
            new BigDecimal("30000.00"), List.of(
                    new PedidoDePagamento.Parte("Ana", "12345678901", "vendedor", new BigDecimal("20000.00")),
                    new PedidoDePagamento.Parte("Bia", "98765432100", "corretor", new BigDecimal("10000.00"))));

    private final List<OrdemPagamentoRequest> recebidas = new ArrayList<>();

    private TesourariaGateway gateway(Function<OrdemPagamentoRequest, OrdemPagamentoResponse> tesouraria) {
        return new TesourariaGateway(request -> {
            recebidas.add(request);
            return tesouraria.apply(request);
        });
    }

    private RespostaDaTesouraria pedirComResposta(OrdemPagamentoResponse resposta) {
        return gateway(request -> resposta).solicitarPagamento(PEDIDO);
    }

    private RespostaDaTesouraria pedirComFalha(RuntimeException falha) {
        return gateway(request -> {
            throw falha;
        }).solicitarPagamento(PEDIDO);
    }

    @Test
    void enviaOPedidoNoContratoDaTesourariaComAIdentificacaoComoRequestId() {
        pedirComResposta(new OrdemPagamentoResponse("ACCEPTED", "778899", null));

        OrdemPagamentoRequest enviada = recebidas.getFirst();
        assertEquals("7c1f0c1e-5d7b-4a52-9d0c-3a3c9a3f3b11", enviada.requestId());
        assertEquals(42L, enviada.processId());
        assertEquals(new BigDecimal("30000.00"), enviada.amount());
        assertEquals(List.of(
                new OrdemPagamentoRequest.Payee("Ana", "12345678901", "vendedor", new BigDecimal("20000.00")),
                new OrdemPagamentoRequest.Payee("Bia", "98765432100", "corretor", new BigDecimal("10000.00"))),
                enviada.payees());
    }

    @Test
    void aceitaViraConfirmadaComOProtocolo() {
        assertEquals(new RespostaDaTesouraria.Confirmada("778899"),
                pedirComResposta(new OrdemPagamentoResponse("ACCEPTED", "778899", null)));
    }

    @Test
    void jaProcessadaValeComoConfirmadaComOProtocolo() {
        assertEquals(new RespostaDaTesouraria.Confirmada("778899"),
                pedirComResposta(new OrdemPagamentoResponse("ALREADY_PROCESSED", "778899", null)));
    }

    @Test
    void rejeitadaViraRecusadaComOMotivo() {
        assertEquals(new RespostaDaTesouraria.Recusada("conta inválida"),
                pedirComResposta(new OrdemPagamentoResponse("REJECTED", null, "conta inválida")));
    }

    @Test
    void confirmacaoSemProtocoloERecusaSemMotivoChegamComOCampoVazioParaODominioDecidir() {
        assertEquals(new RespostaDaTesouraria.Confirmada(null),
                pedirComResposta(new OrdemPagamentoResponse("ACCEPTED", null, null)));
        assertEquals(new RespostaDaTesouraria.Recusada(null),
                pedirComResposta(new OrdemPagamentoResponse("REJECTED", null, null)));
    }

    @Test
    void respostaVaziaOuComStatusDesconhecidoEIncoerente() {
        assertInstanceOf(RespostaDaTesouraria.Incoerente.class, pedirComResposta(null));
        assertInstanceOf(RespostaDaTesouraria.Incoerente.class,
                pedirComResposta(new OrdemPagamentoResponse(null, "778899", null)));
        RespostaDaTesouraria desconhecida = pedirComResposta(new OrdemPagamentoResponse("PENDING", null, null));
        assertInstanceOf(RespostaDaTesouraria.Incoerente.class, desconhecida);
        assertTrue(((RespostaDaTesouraria.Incoerente) desconhecida).descricao().contains("PENDING"));
    }

    @Test
    void erro5xxTimeoutECircuitoAbertoViramSemRespostaSemLancarExcecao() {
        for (RuntimeException falha : List.of(
                new ServerErrorException(Response.Status.SERVICE_UNAVAILABLE),
                new ServerErrorException(Response.Status.INTERNAL_SERVER_ERROR),
                new TimeoutException("demorou"),
                new CircuitBreakerOpenException("circuito aberto"),
                new IllegalStateException("conexão recusada"))) {
            RespostaDaTesouraria resposta = pedirComFalha(falha);

            RespostaDaTesouraria.SemResposta semResposta = assertInstanceOf(RespostaDaTesouraria.SemResposta.class,
                    resposta, falha.toString());
            assertTrue(semResposta.descricao().contains(falha.getClass().getSimpleName()), semResposta.descricao());
        }
    }

    @Test
    void descricaoDaFalhaTemTamanhoLimitado() {
        RespostaDaTesouraria.SemResposta resposta = assertInstanceOf(RespostaDaTesouraria.SemResposta.class,
                pedirComFalha(new IllegalStateException("x".repeat(2000))));

        assertTrue(resposta.descricao().length() <= 300, "" + resposta.descricao().length());
    }
}
