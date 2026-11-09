package br.com.claudio.consorcio.gateway.tesouraria;

/** Resposta da API da tesouraria, no vocabulário dela. Não sai deste pacote. */
public record OrdemPagamentoResponse(String protocolNumber, String status) {
}
