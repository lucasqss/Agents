package br.com.claudio.consorcio.query.entrypoint.web;

import java.math.BigDecimal;

/** Contrato do canal web. Mudanças só aditivas; quebra de contrato vira /web/v2. */
public record CotaWebResponse(
        Long id,
        String numero,
        BigDecimal valorCredito,
        String valorCreditoFormatado,
        Long clienteId,
        String clienteNome,
        ProcessoAtual processoAtual) {

    /** Nulo quando a cota não tem processo. */
    public record ProcessoAtual(Long id, String tipo, String tipoDescricao, String estado, String estadoDescricao) {
    }
}
