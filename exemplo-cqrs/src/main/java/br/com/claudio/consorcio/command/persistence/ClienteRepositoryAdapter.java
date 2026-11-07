package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cliente;
import br.com.claudio.consorcio.command.domain.ClienteRepository;
import br.com.claudio.consorcio.shared.persistence.ClienteEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class ClienteRepositoryAdapter implements ClienteRepository {

    private final ClienteDataRepository dados;

    public ClienteRepositoryAdapter(ClienteDataRepository dados) {
        this.dados = dados;
    }

    /** Stateless session: sem dirty checking, a gravação é sempre explícita (insert ou update). */
    @Override
    public Cliente salvar(Cliente cliente) {
        ClienteEntity entity = ClienteMapper.paraEntidade(cliente);
        if (cliente.id() == null) {
            dados.inserir(entity);
        } else {
            dados.atualizar(entity);
        }
        return ClienteMapper.paraDominio(entity);
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(ClienteMapper::paraDominio);
    }

    @Override
    public boolean existeCpf(String cpf, Long ignorarId) {
        return dados.contarPorCpf(cpf, ignorarId) > 0;
    }

    @Override
    public void excluir(Long id) {
        dados.excluir(id);
    }
}
