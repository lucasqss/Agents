package br.com.claudio.consorcio.persistence.processo;

public record ProcessoResumo(Long id, Long cotaId, String cotaNumero, String tipo, String estado,
        String protocoloPagamento) {
}
