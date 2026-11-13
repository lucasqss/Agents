package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;

public record ProcessoResumo(Long id, Long cotaId, String cotaNumero, String tipo, BigDecimal valor, String estado,
        String protocoloPagamento) {
}
