package br.com.claudio.consorcio.application.cliente;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.cliente.Cliente;
import br.com.claudio.consorcio.domain.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Altera um cliente, mantendo o CPF único. */
@ApplicationScoped
public class AtualizarClienteUseCase {

    private final ClienteRepository clientes;

    public AtualizarClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional
    public void executar(Long id, DadosCliente dados) {
        Cliente cliente = clientes.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
        cliente.alterar(dados.nome(), dados.cpf());
        if (clientes.existeCpf(cliente.cpf(), id)) {
            throw new ConflitoException("Já existe cliente com o CPF " + cliente.cpf());
        }
        clientes.salvar(cliente);
    }
}
