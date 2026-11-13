package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;

/** Um favorecido do processo, como a consulta o devolve. */
public record ProcessoFavorecidoResumo(String nome, String documento, String papel, BigDecimal valor) {
}
