package br.com.claudio.pagamento.persistence.processo;

import br.com.claudio.pagamento.domain.AlteracaoConcorrenteException;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/** Padrão: Repository [PoEAA]; implementa a Separated Interface do domínio sobre o Jakarta Data. */
@ApplicationScoped
public class ProcessoPagamentoRepositoryAdapter implements ProcessoPagamentoRepository {

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
            atualizar(entity);
        }
        for (Transicao transicao : processo.novasTransicoes()) {
            dados.inserirTransicao(ProcessoPagamentoMapper.paraEntidade(entity.getId(), transicao));
        }
        return ProcessoPagamentoMapper.paraDominio(entity);
    }

    /** A versão é conferida no update: quem perdeu a disputa falha aqui, antes de gravar o histórico. */
    private void atualizar(ProcessoPagamentoEntity entity) {
        try {
            dados.atualizar(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new AlteracaoConcorrenteException("Processo", entity.getId());
        }
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(ProcessoPagamentoMapper::paraDominio);
    }
}
