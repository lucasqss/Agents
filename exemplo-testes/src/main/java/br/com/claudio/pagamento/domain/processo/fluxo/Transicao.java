package br.com.claudio.pagamento.domain.processo.fluxo;

import br.com.claudio.pagamento.domain.DominioException;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** R8: registro de uma mudança de estado: de onde, para onde, por qual ação, quando, por quem e com qual observação. */
public record Transicao(EstadoProcesso de, EstadoProcesso para, AcaoProcesso acao, Instant quando, String autor,
        @Nullable String observacao) {

    public static final int TAMANHO_MAXIMO_DA_OBSERVACAO = 500;
    public static final int TAMANHO_MAXIMO_DO_AUTOR = 100;

    public Transicao {
        if (quando == null) {
            throw new DominioException("Data da transição é obrigatória");
        }
        if (autor == null || autor.isBlank()) {
            throw new DominioException("Autor da transição é obrigatório");
        }
        autor = autor.strip();
        if (autor.length() > TAMANHO_MAXIMO_DO_AUTOR) {
            throw new DominioException("Autor tem no máximo " + TAMANHO_MAXIMO_DO_AUTOR + " caracteres");
        }
        observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
        if (observacao != null && observacao.length() > TAMANHO_MAXIMO_DA_OBSERVACAO) {
            throw new DominioException("Observação pode ter no máximo " + TAMANHO_MAXIMO_DA_OBSERVACAO + " caracteres");
        }
    }
}
