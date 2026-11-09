package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Padrão: Service Layer [PoEAA]. Paga o processo e registra a ordem na tesouraria. A transição vem antes
 * da chamada externa, para que uma transição inválida nem chegue à tesouraria; se a tesouraria falhar, a
 * exceção desfaz a transação e o processo continua aprovado. Trade-off: a chamada externa acontece dentro
 * da transação do banco. Em produção, um outbox desacopla as duas coisas.
 */
@ApplicationScoped
public class PagarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final TesourariaGateway tesouraria;

    public PagarProcessoUseCase(ProcessoPagamentoRepository processos, TesourariaGateway tesouraria) {
        this.processos = processos;
        this.tesouraria = tesouraria;
    }

    @Transactional
    public String executar(Long id) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.pagar();
        processo.registrarProtocolo(tesouraria.registrarPagamento(processo.id(), processo.cotaId()));
        return processos.salvar(processo).estado().nome();
    }
}
