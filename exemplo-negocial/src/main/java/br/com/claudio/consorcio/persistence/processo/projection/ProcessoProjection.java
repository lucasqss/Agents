package br.com.claudio.consorcio.persistence.processo.projection;

import java.math.BigDecimal;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ProcessoResumo")
public record ProcessoProjection(Long id, Long cotaId, String cotaNumero, String tipo, BigDecimal valor, String estado,
        String protocoloPagamento) {
}
