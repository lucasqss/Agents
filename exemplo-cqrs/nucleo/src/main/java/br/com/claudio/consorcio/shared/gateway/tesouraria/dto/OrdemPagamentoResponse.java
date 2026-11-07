package br.com.claudio.consorcio.shared.gateway.tesouraria.dto;

/** Resposta da API da tesouraria, no vocabulário dela. Não sai deste pacote. */
public record OrdemPagamentoResponse(String protocolNumber, String status) {
}
