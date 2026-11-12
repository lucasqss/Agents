package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.persistence.processo.ProcessoQueryRepository;
import br.com.claudio.consorcio.persistence.processo.ProcessoTransicaoResumo;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ListarHistoricoProcessoUseCase {

    private final ProcessoQueryRepository repository;

    public ListarHistoricoProcessoUseCase(ProcessoQueryRepository repository) {
        this.repository = repository;
    }

    /** Transições do processo, da mais antiga para a mais recente; vazio se o processo não existe. */
    public Optional<List<ProcessoTransicaoResumo>> executar(Long processoId) {
        if (repository.contar(processoId) == 0) {
            return Optional.empty();
        }
        return Optional.of(repository.historico(processoId));
    }
}
