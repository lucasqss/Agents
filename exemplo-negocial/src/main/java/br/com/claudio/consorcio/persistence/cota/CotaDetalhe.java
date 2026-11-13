package br.com.claudio.consorcio.persistence.cota;

import java.math.BigDecimal;

/** Cota com o nome do cliente e o processo atual (campos do processo nulos se não houver). */
public record CotaDetalhe(
        Long id,
        String numero,
        BigDecimal valorCredito,
        Long clienteId,
        String clienteNome,
        Long processoAtualId,
        String processoAtualTipo,
        String processoAtualEstado) {
}
