package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.UUID;

/**
 * Padrão: Service Layer [PoEAA]. Primeiro passo da saga de pagamento (ADR 002): só grava Pagamento solicitado, com a
 * identificação desta solicitação. Não chama a tesouraria e não depende de ela estar no ar (R7, R8); quem fala com ela é
 * a rotina ({@link ExecutarRotinaDePagamentoUseCase}). A identificação é sorteada aqui, fora do domínio, e entregue
 * pronta. Duas solicitações simultâneas do mesmo processo: a versão deixa uma valer, e a outra é recusada (409).
 */
@ApplicationScoped
public class SolicitarPagamentoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;

    public SolicitarPagamentoProcessoUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    @Transactional
    public EstadoProcesso executar(Long id, String observacao) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.solicitarPagamento(new IdentificacaoSolicitacao(UUID.randomUUID()), Instant.now(), observacao);
        return processos.salvar(processo).estado();
    }
}
