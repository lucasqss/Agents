package br.com.claudio.pagamento.domain.processo;

import java.util.Optional;

/**
 * Padrões: Repository e Separated Interface [PoEAA].
 * Porta do domínio para guardar processos; a implementação fica em persistence.processo.
 */
public interface ProcessoPagamentoRepository {

    /**
     * Grava o processo e as suas novas transições e devolve o processo como ficou gravado (com o id, se era novo).
     * Lança AlteracaoConcorrenteException se o processo mudou no banco depois de lido.
     */
    ProcessoPagamento salvar(ProcessoPagamento processo);

    Optional<ProcessoPagamento> buscarPorId(Long id);
}
