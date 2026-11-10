package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Padrão: Service Layer [PoEAA]. Registra a ordem na tesouraria e paga o processo com o protocolo recebido.
 * O processo fica bloqueado durante a operação, então dois pedidos simultâneos não geram duas ordens: o
 * segundo espera, encontra o processo pago e é recusado. A ação é conferida antes da chamada externa, para
 * que um pedido inválido nem chegue à tesouraria; se a tesouraria falhar, nada é gravado e o processo
 * continua aprovado. Trade-off: a chamada externa acontece dentro da transação do banco. Em produção, um
 * outbox desacopla as duas coisas.
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
    public EstadoProcesso executar(Long id) {
        ProcessoPagamento processo = processos.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.exigirQuePode(AcaoProcesso.PAGAR);
        processo.pagar(tesouraria.registrarPagamento(processo.id(), processo.cotaId()));
        return processos.salvar(processo).estado();
    }
}
