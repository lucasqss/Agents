package br.com.claudio.consorcio.canal.web.entrypoint;

/** Linha da lista de processos do web. Mudanças só aditivas; quebra de contrato vira /web/v2. */
public record ProcessoWebResponse(Long id, Long cotaId, String cotaNumero, String tipo, String tipoDescricao,
        String estado, String estadoDescricao, String protocoloPagamento) {
}
