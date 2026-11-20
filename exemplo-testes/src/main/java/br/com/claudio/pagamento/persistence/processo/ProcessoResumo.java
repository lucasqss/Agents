package br.com.claudio.pagamento.persistence.processo;

import java.math.BigDecimal;

public record ProcessoResumo(Long id, String tipo, String numeroCota, BigDecimal valor, String estado) {
}
