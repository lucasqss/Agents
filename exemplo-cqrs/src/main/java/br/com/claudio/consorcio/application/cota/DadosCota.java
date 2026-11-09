package br.com.claudio.consorcio.application.cota;

import java.math.BigDecimal;

public record DadosCota(String numero, BigDecimal valorCredito, Long clienteId) {
}
