package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;
import java.util.UUID;

/**
 * Padrão: Value Object [PoEAA].
 * Número único de uma solicitação de pagamento (R22). A rotina o repete em todas as tentativas da mesma solicitação,
 * para a tesouraria não abrir duas ordens; a recusa o descarta, e a nova solicitação traz outro. O sorteio do número
 * é de quem chama o domínio (o caso de uso), que o entrega pronto.
 */
public record IdentificacaoSolicitacao(UUID valor) {

    public IdentificacaoSolicitacao {
        if (valor == null) {
            throw new DominioException("Identificação da solicitação de pagamento é obrigatória");
        }
    }

    /** Texto da identificação, como a tesouraria a recebe. */
    public String texto() {
        return valor.toString();
    }
}
