package br.com.claudio.consorcio.persistence.processo.projection;

import java.math.BigDecimal;

/** Valor e motivo da alteração proposta, como a consulta os devolve; os favorecidos vêm de outra consulta. */
public record ProcessoPropostaCabecalhoProjection(BigDecimal valor, String motivo) {
}
