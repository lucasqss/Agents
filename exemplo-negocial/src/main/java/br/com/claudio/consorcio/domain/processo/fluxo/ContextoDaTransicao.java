package br.com.claudio.consorcio.domain.processo.fluxo;

import br.com.claudio.consorcio.domain.Dinheiro;

/**
 * O que a tabela do fluxo precisa saber do processo para escolher o destino de uma linha condicional: o valor vigente e
 * a alçada em vigor. A raiz do agregado só entrega estes dados; quem decide é a {@link CondicaoDaTransicao} declarada na
 * tabela. A alçada é parâmetro de negócio que o domínio não lê: vem de quem chama e pode faltar (nulo) quando a ação não
 * tem linha condicional.
 */
public record ContextoDaTransicao(Dinheiro valor, Dinheiro alcada) {
}
