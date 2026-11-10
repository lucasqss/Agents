package br.com.claudio.consorcio.domain.processo;

import java.util.Optional;

/**
 * Padrões: Repository e Separated Interface [PoEAA].
 * Porta do domínio para guardar processos; a implementação fica em persistence.processo.
 */
public interface ProcessoPagamentoRepository {

    /** Lança AlteracaoConcorrenteException se o processo mudou no banco depois de lido. */
    ProcessoPagamento salvar(ProcessoPagamento processo);

    Optional<ProcessoPagamento> buscarPorId(Long id);

    /** Busca o processo e o mantém bloqueado até o fim da transação, para ações que não podem correr em paralelo. */
    Optional<ProcessoPagamento> buscarPorIdComBloqueio(Long id);

    boolean existePorCota(Long cotaId);

    /** Indica se a cota tem algum processo fora de Pago e Cancelado. */
    boolean existeAtivoPorCota(Long cotaId);
}
