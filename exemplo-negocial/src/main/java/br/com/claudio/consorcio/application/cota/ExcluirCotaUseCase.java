package br.com.claudio.consorcio.application.cota;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Exclui uma cota que não tenha processos. */
@ApplicationScoped
public class ExcluirCotaUseCase {

    private final CotaRepository cotas;
    private final ProcessoPagamentoRepository processos;

    public ExcluirCotaUseCase(CotaRepository cotas, ProcessoPagamentoRepository processos) {
        this.cotas = cotas;
        this.processos = processos;
    }

    @Transactional
    public void executar(Long id) {
        cotas.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Cota", id));
        if (processos.existePorCota(id)) {
            throw new ConflitoException("Cota " + id + " possui processos e não pode ser excluída");
        }
        cotas.excluir(id);
    }
}
