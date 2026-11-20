package br.com.claudio.pagamento.persistence.processo;

import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** Uma linha do histórico do processo, na ordem em que aconteceu (R8). */
public record ProcessoTransicaoResumo(String de, String para, String acao, Instant quando, String autor,
        @Nullable String observacao) {
}
