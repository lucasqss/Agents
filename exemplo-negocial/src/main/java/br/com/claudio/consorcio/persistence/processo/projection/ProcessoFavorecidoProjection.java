package br.com.claudio.consorcio.persistence.processo.projection;

import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/** Um favorecido do processo, como a consulta o devolve. */
@Schema(name = "ProcessoFavorecidoResumo")
public record ProcessoFavorecidoProjection(String nome, String documento, String papel, BigDecimal valor) {
}
