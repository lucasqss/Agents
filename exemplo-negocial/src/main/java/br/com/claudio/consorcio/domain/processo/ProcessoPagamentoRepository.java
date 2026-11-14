package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import java.util.List;
import java.util.Optional;

/**
 * Padrões: Repository e Separated Interface [PoEAA].
 * Porta do domínio para guardar processos; a implementação fica em persistence.processo.
 */
public interface ProcessoPagamentoRepository {

    /** Grava o processo e as suas novas transições. Lança AlteracaoConcorrenteException se o processo mudou no banco depois de lido. */
    ProcessoPagamento salvar(ProcessoPagamento processo);

    Optional<ProcessoPagamento> buscarPorId(Long id);

    /** Busca o processo e o mantém bloqueado até o fim da transação, para ações que não podem correr em paralelo. */
    Optional<ProcessoPagamento> buscarPorIdComBloqueio(Long id);

    /** Ids dos processos que esperam a rotina de pagamento (Pagamento solicitado), do mais antigo ao mais novo. */
    List<Long> idsEmPagamentoSolicitado();

    boolean existePorCota(Long cotaId);

    /** Indica se a cota tem algum processo fora de Pago e Cancelado. */
    boolean existeAtivoPorCota(Long cotaId);

    /** Soma o valor dos processos pagos da cota; zero se não houver nenhum. Só o pago consome crédito (R13, R14). */
    Dinheiro somarValorPagoPorCota(Long cotaId);
}
