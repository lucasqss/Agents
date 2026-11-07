package br.com.claudio.consorcio.canal.taa.entrypoint;

import java.math.BigDecimal;

/** Contrato do terminal de autoatendimento: poucos campos, tudo pronto para a tela do terminal. */
public record CotaTaaResponse(String numero, BigDecimal valorCredito, String valorCreditoFormatado,
        String titular, String situacao) {
}
