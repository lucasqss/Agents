package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/** Padrão: Service Layer [PoEAA]. Cancela o processo e devolve o nome do novo estado. */
@ApplicationScoped
public class CancelarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;

    public CancelarProcessoUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    @Transactional
    public String executar(Long id) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.cancelar();
        return processos.salvar(processo).estado().nome();
    }
}
