package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cliente;
import br.com.claudio.consorcio.shared.persistence.ClienteEntity;

final class ClienteMapper {

    private ClienteMapper() {
    }

    static Cliente paraDominio(ClienteEntity entity) {
        return Cliente.reconstituir(entity.getId(), entity.getNome(), entity.getCpf());
    }

    static void copiarParaEntidade(Cliente cliente, ClienteEntity entity) {
        entity.setNome(cliente.nome());
        entity.setCpf(cliente.cpf());
    }
}
