package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cliente;
import br.com.claudio.consorcio.command.domain.ClienteRepository;
import br.com.claudio.consorcio.shared.persistence.ClienteEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClientePanacheRepository panache;

    public ClienteRepositoryAdapter(ClientePanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    public Cliente salvar(Cliente cliente) {
        ClienteEntity entity = cliente.id() == null ? new ClienteEntity() : panache.findById(cliente.id());
        ClienteMapper.copiarParaEntidade(cliente, entity);
        panache.persist(entity);
        return ClienteMapper.paraDominio(entity);
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return panache.findByIdOptional(id).map(ClienteMapper::paraDominio);
    }

    @Override
    public boolean existeCpf(String cpf, Long ignorarId) {
        return ignorarId == null
                ? panache.count("cpf", cpf) > 0
                : panache.count("cpf = ?1 and id <> ?2", cpf, ignorarId) > 0;
    }

    @Override
    public void excluir(Long id) {
        panache.deleteById(id);
    }
}
