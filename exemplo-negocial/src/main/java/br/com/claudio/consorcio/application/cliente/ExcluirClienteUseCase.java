package br.com.claudio.consorcio.application.cliente;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.cliente.ClienteRepository;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Exclui um cliente que não tenha cotas. */
@ApplicationScoped
public class ExcluirClienteUseCase {

    private final ClienteRepository clientes;
    private final CotaRepository cotas;

    public ExcluirClienteUseCase(ClienteRepository clientes, CotaRepository cotas) {
        this.clientes = clientes;
        this.cotas = cotas;
    }

    @Transactional
    public void executar(Long id) {
        clientes.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cliente", id));
        if (cotas.existePorCliente(id)) {
            throw new ConflitoException("Cliente " + id + " possui cotas e não pode ser excluído");
        }
        clientes.excluir(id);
    }
}
