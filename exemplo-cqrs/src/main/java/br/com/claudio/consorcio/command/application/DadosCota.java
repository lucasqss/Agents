package br.com.claudio.consorcio.command.application;

import java.math.BigDecimal;

public record DadosCota(String numero, BigDecimal valorCredito, Long clienteId) {
}
