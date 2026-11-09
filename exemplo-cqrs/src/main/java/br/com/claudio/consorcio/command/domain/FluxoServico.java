package br.com.claudio.consorcio.command.domain;

import br.com.claudio.consorcio.command.domain.estado.Aprovado;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;

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
