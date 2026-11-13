package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;

/** Valor e motivo da alteração proposta, como a consulta os devolve; os favorecidos vêm de outra consulta. */
public record ProcessoPropostaCabecalho(BigDecimal valor, String motivo) {
}
