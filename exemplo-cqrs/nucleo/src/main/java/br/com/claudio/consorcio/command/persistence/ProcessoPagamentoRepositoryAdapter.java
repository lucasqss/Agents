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
