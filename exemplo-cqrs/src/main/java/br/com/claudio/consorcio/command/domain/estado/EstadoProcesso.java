package br.com.claudio.consorcio.command.domain.estado;

import br.com.claudio.consorcio.command.domain.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.TransicaoInvalidaException;

/**
 * Estado de um processo de pagamento (padrão State). Cada ação devolve o próximo estado;
 * por padrão a ação é inválida e só os estados que a permitem sobrescrevem o método.
 */
public sealed interface EstadoProcesso
        permits Aberto, EmAnalise, AguardandoVistoria, Aprovado, Pago, Cancelado {

    /** Código gravado na coluna {@code estado}. */
    String nome();

    default boolean finalizado() {
        return false;
    }

    default EstadoProcesso iniciarAnalise(ProcessoPagamento processo) {
        throw new TransicaoInvalidaException("iniciar a análise de", nome());
    }

    default EstadoProcesso concluirAnalise(ProcessoPagamento processo) {
        throw new TransicaoInvalidaException("concluir a análise de", nome());
    }

    default EstadoProcesso concluirVistoria(ProcessoPagamento processo) {
        throw new TransicaoInvalidaException("concluir a vistoria de", nome());
    }

    default EstadoProcesso pagar(ProcessoPagamento processo) {
        throw new TransicaoInvalidaException("pagar", nome());
    }

    /** Qualquer estado não final pode ser cancelado. */
    default EstadoProcesso cancelar(ProcessoPagamento processo) {
        if (finalizado()) {
            throw new TransicaoInvalidaException("cancelar", nome());
        }
        return new Cancelado();
    }
}
