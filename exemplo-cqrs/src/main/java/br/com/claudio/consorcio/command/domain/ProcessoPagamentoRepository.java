package br.com.claudio.consorcio.command.domain;

import java.util.Optional;

/**
 * Padrões: Repository e Separated Interface [PoEAA].
 * Porta do domínio para guardar processos; a implementação fica em command.persistence.
 */
public interface ProcessoPagamentoRepository {

    ProcessoPagamento salvar(ProcessoPagamento processo);

    Optional<ProcessoPagamento> buscarPorId(Long id);

    boolean existePorCota(Long cotaId);

    /** Indica se a cota tem algum processo fora de Pago e Cancelado. */
    boolean existeAtivoPorCota(Long cotaId);
}
