package br.com.claudio.consorcio.command.domain;

import br.com.claudio.consorcio.command.domain.estado.AguardandoVistoria;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;

/**
 * Padrão: Strategy, estratégia concreta [GoF].
 * Imobiliário: depois da análise, aguarda vistoria antes de ser aprovado.
 */
public record FluxoImobiliario() implements FluxoProcesso {

    @Override
    public EstadoProcesso aposAnalise() {
        return new AguardandoVistoria();
    }
}
