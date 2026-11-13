package br.com.claudio.consorcio.application.cota;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.cliente.ClienteRepository;
import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Altera uma cota, mantendo cliente existente e número único. */
@ApplicationScoped
public class AtualizarCotaUseCase {

    private final CotaRepository cotas;
    private final ClienteRepository clientes;

    public AtualizarCotaUseCase(CotaRepository cotas, ClienteRepository clientes) {
        this.cotas = cotas;
        this.clientes = clientes;
    }

    @Transactional
    public void executar(Long id, DadosCota dados) {
        Cota cota = cotas.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cota", id));
        cota.alterar(dados.numero(), Dinheiro.de(dados.valorCredito()), dados.clienteId());
        if (clientes.buscarPorId(cota.clienteId()).isEmpty()) {
            throw new NaoEncontradoException("Cliente", cota.clienteId());
        }
        if (cotas.existeNumero(cota.numero(), id)) {
            throw new ConflitoException("Já existe cota com o número " + cota.numero());
        }
        cotas.salvar(cota);
    }
}
