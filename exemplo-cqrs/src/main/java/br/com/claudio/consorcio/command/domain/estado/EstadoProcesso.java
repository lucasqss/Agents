package br.com.claudio.consorcio.command.domain.estado;

import br.com.claudio.consorcio.command.domain.FluxoProcesso;
import br.com.claudio.consorcio.command.domain.TransicaoInvalidaException;
import java.util.List;

/**
 * Padrão: State, interface do estado [GoF].
 * Cada ação devolve o próximo estado; por padrão ela é inválida, e só os estados que a permitem sobrescrevem.
 * Estado novo: crie o record, inclua em {@code permits} e em {@link #todos()}.
 */
public sealed interface EstadoProcesso
        permits Aberto, EmAnalise, AguardandoVistoria, Aprovado, Pago, Cancelado {

    /** Todos os estados; é a lista que o banco usa para reconstruir e para achar os finais. */
    static List<EstadoProcesso> todos() {
        return List.of(new Aberto(), new EmAnalise(), new AguardandoVistoria(), new Aprovado(), new Pago(),
                new Cancelado());
    }

    /** Estado pelo código gravado na coluna {@code estado}. */
    static EstadoProcesso porNome(String nome) {
        return todos().stream()
                .filter(estado -> estado.nome().equals(nome))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Estado desconhecido: " + nome));
    }

    /** Código gravado na coluna {@code estado}. */
    String nome();

    default boolean finalizado() {
        return false;
    }

    default EstadoProcesso iniciarAnalise() {
        throw new TransicaoInvalidaException("iniciar a análise de", nome());
    }

    /** O fluxo do tipo do processo decide o próximo passo. */
    default EstadoProcesso concluirAnalise(FluxoProcesso fluxo) {
        throw new TransicaoInvalidaException("concluir a análise de", nome());
    }

    default EstadoProcesso concluirVistoria() {
        throw new TransicaoInvalidaException("concluir a vistoria de", nome());
    }

    default EstadoProcesso pagar() {
        throw new TransicaoInvalidaException("pagar", nome());
    }

    /** Qualquer estado não final pode ser cancelado. */
    default EstadoProcesso cancelar() {
        if (finalizado()) {
            throw new TransicaoInvalidaException("cancelar", nome());
        }
        return new Cancelado();
    }
}
