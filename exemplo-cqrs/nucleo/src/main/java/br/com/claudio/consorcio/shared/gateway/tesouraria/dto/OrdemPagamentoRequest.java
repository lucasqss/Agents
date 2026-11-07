package br.com.claudio.consorcio.shared.gateway.tesouraria.dto;

/** Corpo da API da tesouraria, no vocabulário dela. Não sai deste pacote. */
public record OrdemPagamentoRequest(String externalReference, Long contractId) {
}
