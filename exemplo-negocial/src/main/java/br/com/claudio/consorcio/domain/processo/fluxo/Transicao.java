package br.com.claudio.consorcio.domain.processo.fluxo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import java.time.Instant;

/**
 * Registro de uma mudança de estado: de onde, para onde, por qual ação, quando e com qual observação.
 * Nas ações que alteram o valor (alterar e propor alteração), traz também o valor vigente antes e o valor pedido;
 * nas demais, os dois são nulos.
 */
public record Transicao(EstadoProcesso de, EstadoProcesso para, AcaoProcesso acao, Instant quando, String observacao,
        Dinheiro valorAnterior, Dinheiro valorNovo) {

    public static final int TAMANHO_MAXIMO_DA_OBSERVACAO = 500;

    public Transicao {
        if (quando == null) {
            throw new DominioException("Data da transição é obrigatória");
        }
        if ((valorAnterior == null) != (valorNovo == null)) {
            throw new DominioException("Transição que altera o valor traz o valor anterior e o novo");
        }
        observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
        if (observacao != null && observacao.length() > TAMANHO_MAXIMO_DA_OBSERVACAO) {
            throw new DominioException("Observação pode ter no máximo " + TAMANHO_MAXIMO_DA_OBSERVACAO + " caracteres");
        }
    }

    /** Transição que não mexe no valor. */
    public Transicao(EstadoProcesso de, EstadoProcesso para, AcaoProcesso acao, Instant quando, String observacao) {
        this(de, para, acao, quando, observacao, null, null);
    }
}
