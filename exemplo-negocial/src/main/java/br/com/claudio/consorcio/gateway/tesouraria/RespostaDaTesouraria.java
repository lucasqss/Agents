package br.com.claudio.consorcio.gateway.tesouraria;

/**
 * O que a tesouraria respondeu, no nosso vocabulário (record próprio do gateway). Toda falha de comunicação é
 * {@link SemResposta}, e o que não segue o contrato é {@link Incoerente}: o gateway não lança exceção para a rotina.
 * Se confirmação e recusa trazem protocolo e motivo é conferido pelo domínio, que decide o que fazer com o que falta (R31).
 */
public sealed interface RespostaDaTesouraria {

    /** ACCEPTED ou ALREADY_PROCESSED: a ordem existe. O protocolo pode vir vazio. */
    record Confirmada(String protocolo) implements RespostaDaTesouraria {
    }

    /** REJECTED. O motivo pode vir vazio. */
    record Recusada(String motivo) implements RespostaDaTesouraria {
    }

    /** Erro 5xx ou outro, demora acima de 2 segundos ou circuito aberto. */
    record SemResposta(String descricao) implements RespostaDaTesouraria {
    }

    /** Resposta vazia ou com status que o contrato não prevê. */
    record Incoerente(String descricao) implements RespostaDaTesouraria {
    }
}
