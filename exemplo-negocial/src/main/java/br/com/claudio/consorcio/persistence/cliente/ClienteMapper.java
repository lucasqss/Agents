package br.com.claudio.consorcio.persistence.cliente;

import br.com.claudio.consorcio.domain.cliente.Cliente;

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
