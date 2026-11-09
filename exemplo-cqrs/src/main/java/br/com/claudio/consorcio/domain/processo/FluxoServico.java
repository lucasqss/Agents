package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.processo.estado.Aprovado;
import br.com.claudio.consorcio.domain.processo.estado.EstadoProcesso;

/**
 * Padrão: Strategy, estratégia concreta [GoF].
 * Serviço: da análise vai direto para aprovado.
 */
public record FluxoServico() implements FluxoProcesso {

    @Override
    public EstadoProcesso aposAnalise() {
        return new Aprovado();
    }
}
