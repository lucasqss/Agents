package br.com.claudio.consorcio.command.application;

import br.com.claudio.consorcio.command.domain.CanalDeAtendimento;
import br.com.claudio.consorcio.command.domain.CotaRepository;
import br.com.claudio.consorcio.command.domain.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class CriarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;

    public CriarProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas) {
        this.processos = processos;
        this.cotas = cotas;
    }

    /** Quem pede vem de qualquer canal; a regra do canal é do domínio (CanalDeAtendimento). */
    @Transactional
    public Long criar(Solicitante solicitante, Long cotaId, TipoProcesso tipo) {
        CanalDeAtendimento.de(solicitante.canal()).validarAbertura(tipo);
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, cotaId);
        cotas.buscarPorId(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        if (processos.existeAtivoPorCota(cotaId)) {
            throw new ConflitoException("Cota " + cotaId + " já possui um processo em andamento");
        }
        return processos.salvar(processo).id();
    }
}
