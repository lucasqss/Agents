package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
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
        } else {
            dados.atualizar(entity);
        }
        return ProcessoPagamentoMapper.paraDominio(entity);
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(ProcessoPagamentoMapper::paraDominio);
    }

    @Override
    public boolean existePorCota(Long cotaId) {
        return dados.contarPorCota(cotaId) > 0;
    }

    @Override
    public boolean existeAtivoPorCota(Long cotaId) {
        return dados.contarAtivosPorCota(cotaId, ESTADOS_FINAIS) > 0;
    }
}
