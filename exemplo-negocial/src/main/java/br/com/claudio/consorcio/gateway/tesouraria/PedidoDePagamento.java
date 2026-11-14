package br.com.claudio.consorcio.gateway.tesouraria;

import java.math.BigDecimal;
import java.util.List;

/**
 * Pedido de pagamento no nosso vocabulário (record próprio do gateway): o caso de uso o monta, o
 * {@link TesourariaGateway} o traduz para o contrato da tesouraria. A {@code identificacao} é a da solicitação.
 */
public record PedidoDePagamento(String identificacao, Long processoId, BigDecimal valor, List<Parte> partes) {

    /** Parte de um favorecido no pedido. */
    public record Parte(String nome, String documento, String papel, BigDecimal valor) {
    }
}
