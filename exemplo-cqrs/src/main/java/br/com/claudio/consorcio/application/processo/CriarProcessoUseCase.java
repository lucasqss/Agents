package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.TipoProcesso;
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
    public Long executar(Long cotaId, TipoProcesso tipo) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, cotaId);
        // O bloqueio coloca em fila as aberturas da mesma cota; sem ele, duas passariam juntas pela verificação abaixo.
        cotas.buscarPorIdComBloqueio(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        if (processos.existeAtivoPorCota(cotaId)) {
            throw new ConflitoException("Cota " + cotaId + " já possui um processo em andamento");
        }
        return processos.salvar(processo).id();
    }
}
