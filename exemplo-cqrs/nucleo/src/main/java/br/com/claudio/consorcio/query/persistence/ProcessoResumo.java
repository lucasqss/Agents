package br.com.claudio.consorcio.query.persistence;

public record ProcessoResumo(Long id, Long cotaId, String cotaNumero, String tipo, String estado,
        String protocoloPagamento) {
}
