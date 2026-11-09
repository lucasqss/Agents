package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.function.Consumer;

/** Padrão: Service Layer [PoEAA]. Aplica uma transição ao processo e devolve o nome do novo estado. */
@ApplicationScoped
public class AvancarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final TesourariaGateway tesouraria;

    public AvancarProcessoUseCase(ProcessoPagamentoRepository processos, TesourariaGateway tesouraria) {
        this.processos = processos;
        this.tesouraria = tesouraria;
    }

    @Transactional
    public String iniciarAnalise(Long id) {
        return aplicar(id, ProcessoPagamento::iniciarAnalise);
    }

    @Transactional
    public String concluirAnalise(Long id) {
        return aplicar(id, ProcessoPagamento::concluirAnalise);
    }

    @Transactional
    public String concluirVistoria(Long id) {
        return aplicar(id, ProcessoPagamento::concluirVistoria);
    }

    /**
     * Paga o processo e registra a ordem na tesouraria. A transição vem antes da chamada externa, para
     * que uma transição inválida nem chegue à tesouraria; se a tesouraria falhar, a exceção desfaz a
     * transação e o processo continua aprovado. Trade-off: a chamada externa acontece dentro da
     * transação do banco. Em produção, um outbox desacopla as duas coisas.
     */
    @Transactional
    public String pagar(Long id) {
        return aplicar(id, processo -> {
            processo.pagar();
            processo.registrarProtocolo(tesouraria.registrarPagamento(processo.id(), processo.cotaId()));
        });
    }

    @Transactional
    public String cancelar(Long id) {
        return aplicar(id, ProcessoPagamento::cancelar);
    }

    private String aplicar(Long id, Consumer<ProcessoPagamento> transicao) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        transicao.accept(processo);
        return processos.salvar(processo).estado().nome();
    }
}
