package br.com.claudio.consorcio.domain.processo.fluxo;

import br.com.claudio.consorcio.domain.DominioException;
import java.time.Instant;

/** Registro de uma mudança de estado: de onde, para onde, por qual ação, quando e com qual observação. */
public record Transicao(EstadoProcesso de, EstadoProcesso para, AcaoProcesso acao, Instant quando, String observacao) {

    public static final int TAMANHO_MAXIMO_DA_OBSERVACAO = 500;

    public Transicao {
        if (quando == null) {
            throw new DominioException("Data da transição é obrigatória");
        }
        observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
        if (observacao != null && observacao.length() > TAMANHO_MAXIMO_DA_OBSERVACAO) {
            throw new DominioException("Observação pode ter no máximo " + TAMANHO_MAXIMO_DA_OBSERVACAO + " caracteres");
        }
    }
}
