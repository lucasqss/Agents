package br.com.claudio.consorcio.persistence.processo;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Uma linha do histórico do processo, na ordem em que aconteceu, com quem a fez ({@code autor}: o usuário, ou "sistema"
 * nas da rotina de pagamento; "desconhecido" nas anteriores à identidade, R19). Os valores só vêm nas ações que alteram o valor
 * (alterar e propor alteração); nas demais são nulos.
 */
public record ProcessoTransicaoResumo(String de, String para, String acao, Instant quando, String autor, String observacao,
        BigDecimal valorAnterior, BigDecimal valorNovo) {
}
