package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import java.util.List;

/**
 * Padrão: Value Object [PoEAA]. Alteração de valor e favorecidos proposta pelo backoffice, guardada à parte até a
 * resposta do cliente (R25 a R27): só vale se ele concordar, e o processo não a conhece enquanto isso. Tem as mesmas
 * invariantes do valor e dos favorecidos do processo (R12, R15, R29) e o motivo, obrigatório.
 */
public record AlteracaoProposta(Dinheiro valor, List<Favorecido> favorecidos, String motivo) {

    public AlteracaoProposta {
        ProcessoPagamento.exigirDivisaoValida(valor, favorecidos);
        if (motivo == null || motivo.isBlank()) {
            throw new DominioException("Motivo da alteração é obrigatório");
        }
        motivo = motivo.strip();
        if (motivo.length() > Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) {
            throw new DominioException("Motivo pode ter no máximo " + Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO + " caracteres");
        }
        favorecidos = List.copyOf(favorecidos);
    }
}
