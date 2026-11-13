package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.AlteracaoConcorrenteException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/** Padrão: Repository [PoEAA]; implementa a Separated Interface do domínio sobre o Jakarta Data. */
@ApplicationScoped
public class ProcessoPagamentoRepositoryAdapter implements ProcessoPagamentoRepository {

    private static final List<String> ESTADOS_FINAIS = Arrays.stream(EstadoProcesso.values())
            .filter(EstadoProcesso::finalizado)
            .map(EstadoProcesso::name)
            .toList();

    private final ProcessoPagamentoDataRepository dados;

    public ProcessoPagamentoRepositoryAdapter(ProcessoPagamentoDataRepository dados) {
        this.dados = dados;
    }

    @Override
    public ProcessoPagamento salvar(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = ProcessoPagamentoMapper.paraEntidade(processo);
        if (processo.id() == null) {
            dados.inserir(entity);
            // Os favorecidos nunca mudam depois da abertura nesta entrega: são gravados uma vez, com o processo novo.
            for (ProcessoFavorecidoEntity favorecido : ProcessoPagamentoMapper.paraEntidades(entity.getId(),
                    processo.favorecidos())) {
                dados.inserirFavorecido(favorecido);
            }
        } else {
            atualizar(entity);
        }
        for (Transicao transicao : processo.novasTransicoes()) {
            dados.inserirTransicao(ProcessoPagamentoMapper.paraEntidade(entity.getId(), transicao));
        }
        return reconstituir(entity);
    }

    private ProcessoPagamento reconstituir(ProcessoPagamentoEntity entity) {
        return ProcessoPagamentoMapper.paraDominio(entity, dados.buscarFavorecidos(entity.getId()));
    }

    private void atualizar(ProcessoPagamentoEntity entity) {
        try {
            dados.atualizar(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new AlteracaoConcorrenteException("Processo", entity.getId());
        }
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(this::reconstituir);
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorIdComBloqueio(Long id) {
        return dados.buscarPorIdComBloqueio(id).map(this::reconstituir);
    }

    @Override
    public boolean existePorCota(Long cotaId) {
        return dados.contarPorCota(cotaId) > 0;
    }

    @Override
    public boolean existeAtivoPorCota(Long cotaId) {
        return dados.contarAtivosPorCota(cotaId, ESTADOS_FINAIS) > 0;
    }

    @Override
    public Dinheiro somarValorPagoPorCota(Long cotaId) {
        BigDecimal soma = dados.somarValorPorCotaEEstado(cotaId, EstadoProcesso.PAGO.name());
        return soma == null ? Dinheiro.ZERO : Dinheiro.de(soma);
    }
}
