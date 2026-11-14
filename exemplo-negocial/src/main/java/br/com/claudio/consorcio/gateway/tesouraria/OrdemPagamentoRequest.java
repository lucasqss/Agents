package br.com.claudio.consorcio.gateway.tesouraria;

import java.math.BigDecimal;
import java.util.List;

/**
 * Corpo da API da tesouraria, no vocabulário dela. Não sai deste pacote. {@code requestId} é a chave de idempotência:
 * a mesma em todas as tentativas da mesma solicitação.
 */
public record OrdemPagamentoRequest(String requestId, Long processId, BigDecimal amount, List<Payee> payees) {

    /** Parte de um favorecido na ordem. */
    public record Payee(String name, String document, String role, BigDecimal amount) {
    }
}
