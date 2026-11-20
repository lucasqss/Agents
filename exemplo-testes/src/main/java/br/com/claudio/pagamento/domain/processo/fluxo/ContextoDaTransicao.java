package br.com.claudio.pagamento.domain.processo.fluxo;

import br.com.claudio.pagamento.domain.Dinheiro;
import org.jspecify.annotations.Nullable;

/**
 * O que a tabela do fluxo precisa saber do processo para escolher o destino de uma linha condicional: o valor e a alçada
 * em vigor. A alçada é parâmetro de negócio que o domínio não lê: vem de quem chama e pode faltar (nulo) quando a ação
 * não tem linha condicional.
 */
public record ContextoDaTransicao(Dinheiro valor, @Nullable Dinheiro alcada) {
}
