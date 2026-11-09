package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.processo.estado.AguardandoVistoria;
import br.com.claudio.consorcio.domain.processo.estado.EstadoProcesso;

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
