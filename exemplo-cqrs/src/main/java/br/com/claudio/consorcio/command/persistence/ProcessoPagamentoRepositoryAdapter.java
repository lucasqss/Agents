package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.command.domain.estado.Cancelado;
import br.com.claudio.consorcio.command.domain.estado.Pago;
import br.com.claudio.consorcio.shared.persistence.ProcessoPagamentoEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ProcessoPagamentoRepositoryAdapter implements ProcessoPagamentoRepository {

    private static final List<String> ESTADOS_FINAIS = List.of(Pago.NOME, Cancelado.NOME);

    private final ProcessoPagamentoPanacheRepository panache;

    public ProcessoPagamentoRepositoryAdapter(ProcessoPagamentoPanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    public ProcessoPagamento salvar(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = processo.id() == null
                ? new ProcessoPagamentoEntity()
                : panache.findById(processo.id());
        ProcessoPagamentoMapper.copiarParaEntidade(processo, entity);
        panache.persist(entity);
        return ProcessoPagamentoMapper.paraDominio(entity);
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        return panache.findByIdOptional(id).map(ProcessoPagamentoMapper::paraDominio);
    }

    @Override
    public boolean existePorCota(Long cotaId) {
        return panache.count("cotaId", cotaId) > 0;
    }

    @Override
    public boolean existeAtivoPorCota(Long cotaId) {
        return panache.count("cotaId = ?1 and estado not in ?2", cotaId, ESTADOS_FINAIS) > 0;
    }
}
