package br.com.claudio.consorcio.gateway.tesouraria;

/**
 * Resposta da API da tesouraria, no vocabulário dela. Não sai deste pacote. {@code status} é ACCEPTED,
 * REJECTED ou ALREADY_PROCESSED; {@code protocolNumber} vem nas confirmações e {@code reason} nas recusas.
 */
public record OrdemPagamentoResponse(String status, String protocolNumber, String reason) {
}
