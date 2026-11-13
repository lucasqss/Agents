package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

/**
 * Padrão: Service Layer [PoEAA]. Aplica ao processo uma ação que só muda o estado, registra-a no
 * histórico e devolve o novo estado.
 * Ação com efeito fora do processo (ex.: pagar, que chama a tesouraria) tem o seu próprio use case.
 */
@ApplicationScoped
public class ExecutarAcaoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;

    public ExecutarAcaoProcessoUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    @Transactional
    public EstadoProcesso executar(Long id, AcaoProcesso acao, String observacao) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.executar(acao, Instant.now(), observacao);
        return processos.salvar(processo).estado();
    }
}
