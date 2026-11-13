package br.com.claudio.consorcio.application.cliente;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.domain.cliente.Cliente;
import br.com.claudio.consorcio.domain.cliente.ClienteRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Cria um cliente, com CPF único. */
@ApplicationScoped
public class CriarClienteUseCase {

    private final ClienteRepository clientes;

    public CriarClienteUseCase(ClienteRepository clientes) {
        this.clientes = clientes;
    }

    @Transactional
    public Long executar(DadosCliente dados) {
        Cliente cliente = Cliente.novo(dados.nome(), dados.cpf());
        if (clientes.existeCpf(cliente.cpf(), null)) {
            throw new ConflitoException("Já existe cliente com o CPF " + cliente.cpf());
        }
        return clientes.salvar(cliente).id();
    }
}
