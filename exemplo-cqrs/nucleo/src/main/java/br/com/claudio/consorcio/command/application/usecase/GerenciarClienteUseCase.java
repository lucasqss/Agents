package br.com.claudio.consorcio.command.application.usecase;

import br.com.claudio.consorcio.command.application.dto.DadosCliente;
import br.com.claudio.consorcio.command.application.exception.ConflitoException;
import br.com.claudio.consorcio.command.application.exception.NaoEncontradoException;
import br.com.claudio.consorcio.command.domain.entity.Cliente;
import br.com.claudio.consorcio.command.domain.repository.ClienteRepository;
import br.com.claudio.consorcio.command.domain.repository.CotaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GerenciarClienteUseCase {

    private final ClienteRepository clientes;
    private final CotaRepository cotas;

    public GerenciarClienteUseCase(ClienteRepository clientes, CotaRepository cotas) {
        this.clientes = clientes;
        this.cotas = cotas;
    }

    @Transactional
    public Long criar(DadosCliente dados) {
        Cliente cliente = Cliente.novo(dados.nome(), dados.cpf());
        validarCpfUnico(cliente.cpf(), null);
        return clientes.salvar(cliente).id();
    }

    @Transactional
    public void atualizar(Long id, DadosCliente dados) {
        Cliente cliente = clientes.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
        cliente.alterar(dados.nome(), dados.cpf());
        validarCpfUnico(cliente.cpf(), id);
        clientes.salvar(cliente);
    }

    @Transactional
    public void excluir(Long id) {
        clientes.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
        if (cotas.existePorCliente(id)) {
            throw new ConflitoException("Cliente " + id + " possui cotas e não pode ser excluído");
        }
        clientes.excluir(id);
    }

    private void validarCpfUnico(String cpf, Long ignorarId) {
        if (clientes.existeCpf(cpf, ignorarId)) {
            throw new ConflitoException("Já existe cliente com o CPF " + cpf);
        }
    }
}
