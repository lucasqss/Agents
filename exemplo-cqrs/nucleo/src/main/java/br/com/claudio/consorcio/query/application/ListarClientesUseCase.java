package br.com.claudio.consorcio.query.application;

import br.com.claudio.consorcio.query.persistence.ClienteQueryRepository;
import br.com.claudio.consorcio.query.persistence.ClienteResumo;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ListarClientesUseCase {

    private final ClienteQueryRepository repository;

    public ListarClientesUseCase(ClienteQueryRepository repository) {
        this.repository = repository;
    }

    public List<ClienteResumo> executar() {
        return repository.listarComQuantidadeDeCotas();
    }
}
