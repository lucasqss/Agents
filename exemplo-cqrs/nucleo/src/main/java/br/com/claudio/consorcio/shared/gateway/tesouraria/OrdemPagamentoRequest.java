package br.com.claudio.consorcio.shared.gateway.tesouraria;

/** Corpo da API da tesouraria, no vocabulário dela. Não sai deste pacote. */
public record OrdemPagamentoRequest(String externalReference, Long contractId) {
}
