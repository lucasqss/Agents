package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;
import java.util.List;

/** A alteração proposta que aguarda o cliente: novo valor, novos favorecidos e o motivo do backoffice. */
public record ProcessoPropostaResumo(BigDecimal valor, List<ProcessoFavorecidoResumo> favorecidos, String motivo) {
}
