package br.com.claudio.consorcio.persistence.processo.projection;

import java.math.BigDecimal;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** A alteração proposta que aguarda o cliente: novo valor, novos favorecidos e o motivo do backoffice. */
@Schema(name = "ProcessoPropostaResumo")
public record ProcessoPropostaProjection(BigDecimal valor, List<ProcessoFavorecidoProjection> favorecidos, String motivo) {
}
