package br.com.claudio.consorcio.command.domain.repository;

import br.com.claudio.consorcio.command.domain.entity.Cliente;
import java.util.Optional;

public interface ClienteRepository {

    /** Insere (id nulo) ou atualiza o cliente e devolve o cliente com id. */
    Cliente salvar(Cliente cliente);

    Optional<Cliente> buscarPorId(Long id);

    /** Indica se já existe outro cliente com o CPF; {@code ignorarId} exclui o próprio cliente da busca. */
    boolean existeCpf(String cpf, Long ignorarId);

    void excluir(Long id);
}
