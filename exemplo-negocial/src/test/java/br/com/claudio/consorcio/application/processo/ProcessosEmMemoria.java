package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.Documento;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Repositório de processos em memória para os testes dos use cases. {@code falhaAoLer} simula um erro de banco só para
 * aquele processo.
 */
final class ProcessosEmMemoria implements ProcessoPagamentoRepository {

    static final Dinheiro VALOR = Dinheiro.de("1000.00");

    final Map<Long, ProcessoPagamento> gravados = new LinkedHashMap<>();
    final Set<Long> falhaAoLer = new HashSet<>();
    /** Quantas vezes {@link #salvar} foi chamado: o objeto em memória muda mesmo sem gravar, o banco não. */
    int salvamentos;

    ProcessoPagamento solicitado(long id) {
        ProcessoPagamento processo = ProcessoPagamento.reconstituir(id, 1L, TipoProcesso.SERVICO, VALOR,
                List.of(new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", VALOR)),
                EstadoProcesso.PAGAMENTO_SOLICITADO, null, null, new IdentificacaoSolicitacao(UUID.randomUUID()), null, 0);
        gravados.put(id, processo);
        return processo;
    }

    @Override
    public ProcessoPagamento salvar(ProcessoPagamento processo) {
        salvamentos++;
        gravados.put(processo.id(), processo);
        return processo;
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        if (falhaAoLer.contains(id)) {
            throw new IllegalStateException("banco fora do ar para o processo " + id);
        }
        return Optional.ofNullable(gravados.get(id));
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorIdComBloqueio(Long id) {
        return Optional.ofNullable(gravados.get(id));
    }

    @Override
    public List<Long> idsEmPagamentoSolicitado() {
        return gravados.values().stream().filter(p -> p.estado() == EstadoProcesso.PAGAMENTO_SOLICITADO)
                .map(ProcessoPagamento::id).toList();
    }

    @Override
    public boolean existePorCota(Long cotaId) {
        return false;
    }

    @Override
    public boolean existeAtivoPorCota(Long cotaId) {
        return false;
    }

    @Override
    public Dinheiro somarValorPagoPorCota(Long cotaId) {
        return Dinheiro.ZERO;
    }
}
