package br.com.claudio.consorcio.persistence.processo;

import java.time.Instant;

/** Uma linha do histórico do processo, na ordem em que aconteceu. */
public record ProcessoTransicaoResumo(String de, String para, String acao, Instant quando, String observacao) {
}
