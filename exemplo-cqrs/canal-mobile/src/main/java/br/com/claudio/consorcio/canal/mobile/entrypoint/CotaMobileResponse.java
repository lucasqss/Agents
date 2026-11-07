package br.com.claudio.consorcio.canal.mobile.entrypoint;

import java.math.BigDecimal;

/** Contrato do card de cota no app: só o que a tela mostra. */
public record CotaMobileResponse(
        Long id,
        String numero,
        BigDecimal valorCredito,
        String valorCreditoFormatado,
        String titular,
        String estado,
        String situacao) {
}
