package br.com.claudio.consorcio.command.persistence.mapper;

import br.com.claudio.consorcio.command.domain.entity.Cliente;
import br.com.claudio.consorcio.shared.persistence.ClienteEntity;

public final class ClienteMapper {

    private ClienteMapper() {
    }

    public static Cliente paraDominio(ClienteEntity entity) {
        return Cliente.reconstituir(entity.getId(), entity.getNome(), entity.getCpf());
    }

    public static ClienteEntity paraEntidade(Cliente cliente) {
        ClienteEntity entity = new ClienteEntity();
        entity.setId(cliente.id());
        entity.setNome(cliente.nome());
        entity.setCpf(cliente.cpf());
        return entity;
    }
}
