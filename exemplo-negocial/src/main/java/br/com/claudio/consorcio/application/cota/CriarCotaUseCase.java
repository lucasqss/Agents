package br.com.claudio.consorcio.application.cota;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.cliente.ClienteRepository;
import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Cria uma cota de um cliente existente, com número único. */
@ApplicationScoped
public class CriarCotaUseCase {

    private final CotaRepository cotas;
    private final ClienteRepository clientes;

    public CriarCotaUseCase(CotaRepository cotas, ClienteRepository clientes) {
        this.cotas = cotas;
        this.clientes = clientes;
    }

    @Transactional
    public Long executar(DadosCota dados) {
        Cota cota = Cota.nova(dados.numero(), Dinheiro.de(dados.valorCredito()), dados.clienteId());
        if (clientes.buscarPorId(cota.clienteId()).isEmpty()) {
            throw new NaoEncontradoException("Cliente", cota.clienteId());
        }
        if (cotas.existeNumero(cota.numero(), null)) {
            throw new ConflitoException("Já existe cota com o número " + cota.numero());
        }
        return cotas.salvar(cota).id();
    }
}
