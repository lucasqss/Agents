package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.processo.estado.EstadoProcesso;

/**
 * Padrão: Strategy, interface da estratégia [GoF].
 * Pontos em que o fluxo de um tipo de processo foge do caminho padrão; um desvio novo vira método aqui.
 */
public interface FluxoProcesso {

    /** Estado para onde o processo vai quando a análise é concluída. */
    EstadoProcesso aposAnalise();
}
