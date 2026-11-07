package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cliente;
import br.com.claudio.consorcio.shared.persistence.ClienteEntity;

final class ClienteMapper {

    private ClienteMapper() {
    }

    static Cliente paraDominio(ClienteEntity entity) {
        return Cliente.reconstituir(entity.getId(), entity.getNome(), entity.getCpf());
    }

    static ClienteEntity paraEntidade(Cliente cliente) {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(cliente.id());
        entity.setNome(cliente.nome());
        entity.setCpf(cliente.cpf());
        return entity;
    }
}
