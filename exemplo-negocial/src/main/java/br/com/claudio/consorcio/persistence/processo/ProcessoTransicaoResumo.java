package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uma linha do histórico do processo, na ordem em que aconteceu. Os valores só vêm nas ações que alteram o valor
 * (alterar e propor alteração); nas demais são nulos.
 */
public record ProcessoTransicaoResumo(String de, String para, String acao, Instant quando, String observacao,
        BigDecimal valorAnterior, BigDecimal valorNovo) {
}
