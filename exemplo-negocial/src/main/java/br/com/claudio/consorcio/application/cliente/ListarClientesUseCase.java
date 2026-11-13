package br.com.claudio.consorcio.application.cliente;

import br.com.claudio.consorcio.persistence.cliente.ClienteQueryRepository;
import br.com.claudio.consorcio.persistence.cliente.ClienteResumo;
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
