package br.com.claudio.consorcio.command.application;

import br.com.claudio.consorcio.command.domain.CotaRepository;
import br.com.claudio.consorcio.command.domain.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Cria um processo novo para uma cota sem processo em andamento. */
@ApplicationScoped
public class CriarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;

    public CriarProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas) {
        this.processos = processos;
        this.cotas = cotas;
    }

    @Transactional
    public Long criar(Long cotaId, TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, cotaId);
        cotas.buscarPorId(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        if (processos.existeAtivoPorCota(cotaId)) {
            throw new ConflitoException("Cota " + cotaId + " já possui um processo em andamento");
        }
        return processos.salvar(processo).id();
    }
}
