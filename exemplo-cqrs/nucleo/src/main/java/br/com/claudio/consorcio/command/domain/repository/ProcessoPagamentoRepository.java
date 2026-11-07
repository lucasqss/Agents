package br.com.claudio.consorcio.command.domain.repository;

import br.com.claudio.consorcio.command.domain.entity.pagamento.ProcessoPagamento;
import java.util.Optional;

public interface ProcessoPagamentoRepository {

    ProcessoPagamento salvar(ProcessoPagamento processo);

    Optional<ProcessoPagamento> buscarPorId(Long id);

    boolean existePorCota(Long cotaId);

    /** Indica se a cota tem algum processo fora de Pago e Cancelado. */
    boolean existeAtivoPorCota(Long cotaId);
}
