package br.com.claudio.consorcio.command.application.usecase;

import br.com.claudio.consorcio.command.application.dto.DadosCota;
import br.com.claudio.consorcio.command.application.exception.ConflitoException;
import br.com.claudio.consorcio.command.application.exception.NaoEncontradoException;
import br.com.claudio.consorcio.command.domain.entity.Cota;
import br.com.claudio.consorcio.command.domain.repository.ClienteRepository;
import br.com.claudio.consorcio.command.domain.repository.CotaRepository;
import br.com.claudio.consorcio.command.domain.repository.ProcessoPagamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class GerenciarCotaUseCase {

    private final CotaRepository cotas;
    private final ClienteRepository clientes;
    private final ProcessoPagamentoRepository processos;

    public GerenciarCotaUseCase(CotaRepository cotas, ClienteRepository clientes,
            ProcessoPagamentoRepository processos) {
        this.cotas = cotas;
        this.clientes = clientes;
        this.processos = processos;
    }

    @Transactional
    public Long criar(DadosCota dados) {
        Cota cota = Cota.nova(dados.numero(), dados.valorCredito(), dados.clienteId());
        validar(cota, null);
        return cotas.salvar(cota).id();
    }

    @Transactional
    public void atualizar(Long id, DadosCota dados) {
        Cota cota = cotas.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cota", id));
        cota.alterar(dados.numero(), dados.valorCredito(), dados.clienteId());
        validar(cota, id);
        cotas.salvar(cota);
    }

    @Transactional
    public void excluir(Long id) {
        cotas.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cota", id));
        if (processos.existePorCota(id)) {
            throw new ConflitoException("Cota " + id + " possui processos e não pode ser excluída");
        }
        cotas.excluir(id);
    }

    private void validar(Cota cota, Long ignorarId) {
        if (clientes.buscarPorId(cota.clienteId()).isEmpty()) {
            throw new NaoEncontradoException("Cliente", cota.clienteId());
        }
        if (cotas.existeNumero(cota.numero(), ignorarId)) {
            throw new ConflitoException("Já existe cota com o número " + cota.numero());
        }
    }
}
