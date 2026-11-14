package br.com.claudio.consorcio.domain.processo.fluxo;

/**
 * Condição que escolhe entre dois destinos de uma mesma linha da tabela do fluxo (R16). Fica declarada na tabela, ao lado
 * da linha, e é avaliada com o {@link ContextoDaTransicao}. Padrão: State dirigido por tabela [GoF], com guarda na linha.
 */
@FunctionalInterface
public interface CondicaoDaTransicao {

    boolean vale(ContextoDaTransicao contexto);

    /** R16: o valor vigente passa da alçada. Valor igual à alçada não passa. */
    CondicaoDaTransicao VALOR_PASSA_DA_ALCADA = contexto -> {
        if (contexto.alcada() == null) {
            throw new IllegalStateException("A alçada é necessária para decidir o destino desta transição");
        }
        return contexto.valor().maiorQue(contexto.alcada());
    };
}
