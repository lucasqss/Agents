package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.persistence.processo.ProcessoPropostaResumo;
import br.com.claudio.consorcio.persistence.processo.ProcessoQueryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ConsultarAlteracaoPropostaUseCase {

    private final ProcessoQueryRepository repository;

    public ConsultarAlteracaoPropostaUseCase(ProcessoQueryRepository repository) {
        this.repository = repository;
    }

    /** A alteração que aguarda a resposta do cliente; vazio sem proposta pendente ou com processo inexistente. */
    public Optional<ProcessoPropostaResumo> executar(Long processoId) {
        return repository.propostaPendente(processoId);
    }
}
