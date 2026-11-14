package br.com.claudio.consorcio.entrypoint.web.cota;

import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Contrato do canal web. Mudanças só aditivas; quebra de contrato vira /web/v2. */
public record CotaWebResponse(
        Long id,
        String numero,
        BigDecimal valorCredito,
        String valorCreditoFormatado,
        Long clienteId,
        String clienteNome,
        @Nullable ProcessoAtual processoAtual) {

    /** Nulo quando a cota não tem processo. */
    public record ProcessoAtual(Long id, String tipo, String tipoDescricao, String estado, String estadoDescricao) {
    }
}
