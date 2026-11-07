package br.com.claudio.consorcio.shared.integracao.tesouraria;

/** Corpo da API da tesouraria, no vocabulário dela. Não sai deste pacote. */
public record OrdemPagamentoRequest(String externalReference, Long contractId) {
}
