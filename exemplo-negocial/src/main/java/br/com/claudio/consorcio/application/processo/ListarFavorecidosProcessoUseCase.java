package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.persistence.processo.ProcessoFavorecidoResumo;
import br.com.claudio.consorcio.persistence.processo.ProcessoQueryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class ListarFavorecidosProcessoUseCase {

    private final ProcessoQueryRepository repository;

    public ListarFavorecidosProcessoUseCase(ProcessoQueryRepository repository) {
        this.repository = repository;
    }

    /** Favorecidos do processo, na ordem em que foram informados; vazio se o processo não existe. */
    public Optional<List<ProcessoFavorecidoResumo>> executar(Long processoId) {
        if (repository.contar(processoId) == 0) {
            return Optional.empty();
        }
        return Optional.of(repository.favorecidos(processoId));
    }
}
