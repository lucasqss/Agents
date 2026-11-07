package br.com.claudio.consorcio.command.application.dto;

import java.math.BigDecimal;

public record DadosCota(String numero, BigDecimal valorCredito, Long clienteId) {
}
