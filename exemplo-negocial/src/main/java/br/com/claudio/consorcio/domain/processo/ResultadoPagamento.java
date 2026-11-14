package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;

/**
 * Resultado de uma tentativa de pagamento, já no nosso vocabulário: o que a tesouraria respondeu, ou que não respondeu.
 * Resposta que não faz sentido (confirmação sem protocolo, recusa sem motivo) nunca é aplicada: vira {@link Incoerente}
 * (R31). Quem traduz o retorno do sistema externo usa as fábricas {@link #confirmado} e {@link #recusado}.
 */
public sealed interface ResultadoPagamento {

    /** A tesouraria fez a ordem e emitiu o protocolo (inclusive quando ela diz que já tinha feito). */
    record Confirmado(String protocolo) implements ResultadoPagamento {

        public Confirmado {
            if (protocolo == null || protocolo.isBlank()) {
                throw new DominioException("Protocolo de pagamento é obrigatório");
            }
        }
    }

    /** A tesouraria recusou, com o motivo. */
    record Recusado(String motivo) implements ResultadoPagamento {

        public Recusado {
            if (motivo == null || motivo.isBlank()) {
                throw new DominioException("Motivo da recusa é obrigatório");
            }
        }
    }

    /** Indisponível, sem resposta no prazo ou circuito aberto: o processo continua esperando. */
    record SemResposta(String descricao) implements ResultadoPagamento {

        public SemResposta {
            if (descricao == null || descricao.isBlank()) {
                throw new DominioException("Descrição da tentativa sem resposta é obrigatória");
            }
        }
    }

    /** Resposta que não faz sentido (R31): não é aplicada, e o fato vai ao histórico. */
    record Incoerente(String descricao) implements ResultadoPagamento {

        public Incoerente {
            if (descricao == null || descricao.isBlank()) {
                throw new DominioException("Descrição da resposta incoerente é obrigatória");
            }
        }
    }

    /** Confirmação com protocolo; sem protocolo, a resposta é incoerente. */
    static ResultadoPagamento confirmado(String protocolo) {
        return protocolo == null || protocolo.isBlank()
                ? new Incoerente("A tesouraria confirmou o pagamento sem informar o protocolo")
                : new Confirmado(protocolo.strip());
    }

    /** Recusa com motivo; sem motivo, a resposta é incoerente. */
    static ResultadoPagamento recusado(String motivo) {
        return motivo == null || motivo.isBlank()
                ? new Incoerente("A tesouraria recusou o pagamento sem informar o motivo")
                : new Recusado(motivo.strip());
    }
}
