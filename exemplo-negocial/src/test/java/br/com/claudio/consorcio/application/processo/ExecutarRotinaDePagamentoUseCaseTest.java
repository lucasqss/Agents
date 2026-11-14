package br.com.claudio.consorcio.application.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.Documento;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.gateway.tesouraria.OrdemPagamentoResponse;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** A rotina isola a falha de um processo: os demais seguem, e o que falhou continua esperando a próxima rodada. */
class ExecutarRotinaDePagamentoUseCaseTest {

    private static final Dinheiro VALOR = Dinheiro.de("1000.00");

    /** Repositório em memória; {@code falhaAoLer} simula um erro de banco só para aquele processo. */
    private static final class RepositorioEmMemoria implements ProcessoPagamentoRepository {

        private final Map<Long, ProcessoPagamento> gravados = new LinkedHashMap<>();
        private final Set<Long> falhaAoLer = new java.util.HashSet<>();

        ProcessoPagamento solicitado(long id) {
            ProcessoPagamento processo = ProcessoPagamento.reconstituir(id, 1L, TipoProcesso.SERVICO, VALOR,
                    List.of(new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", VALOR)),
                    EstadoProcesso.PAGAMENTO_SOLICITADO, null, null, new IdentificacaoSolicitacao(UUID.randomUUID()), 0);
            gravados.put(id, processo);
            return processo;
        }

        @Override
        public ProcessoPagamento salvar(ProcessoPagamento processo) {
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

    private final RepositorioEmMemoria repositorio = new RepositorioEmMemoria();
    private final List<String> pedidos = new ArrayList<>();

    private ExecutarRotinaDePagamentoUseCase rotinaComTesouraria(OrdemPagamentoResponse resposta) {
        TesourariaGateway gateway = new TesourariaGateway(request -> {
            pedidos.add(request.requestId() + "|" + request.processId());
            return resposta;
        });
        return new ExecutarRotinaDePagamentoUseCase(repositorio, gateway, new AplicarResultadoPagamentoUseCase(repositorio));
    }

    @Test
    void falhaEmUmProcessoNaoImpedeOsDemais() {
        repositorio.solicitado(1);
        repositorio.solicitado(2);
        repositorio.solicitado(3);
        repositorio.falhaAoLer.add(2L);

        rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null)).executar();

        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(1L).estado());
        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, repositorio.gravados.get(2L).estado());
        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(3L).estado());
        assertEquals(2, pedidos.size());
    }

    @Test
    void oProcessoQueFalhouEhTentadoDeNovoNaProximaRodada() {
        repositorio.solicitado(1);
        repositorio.falhaAoLer.add(1L);
        ExecutarRotinaDePagamentoUseCase rotina = rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null));
        rotina.executar();
        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, repositorio.gravados.get(1L).estado());

        repositorio.falhaAoLer.clear();
        rotina.executar();

        assertEquals(EstadoProcesso.PAGO, repositorio.gravados.get(1L).estado());
    }

    @Test
    void cadaRodadaRepeteAMesmaIdentificacaoEORespostaIncoerenteViraTentativa() {
        ProcessoPagamento processo = repositorio.solicitado(7);
        ExecutarRotinaDePagamentoUseCase rotina = rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", null, null));

        rotina.executar();
        rotina.executar();

        assertEquals(EstadoProcesso.PAGAMENTO_SOLICITADO, processo.estado());
        assertEquals(2, pedidos.size());
        assertEquals(pedidos.get(0), pedidos.get(1), "mesma identificação, mesmo processo");
        assertTrue(pedidos.get(0).startsWith(processo.identificacaoSolicitacao().orElseThrow().texto()));
        assertEquals(2, processo.novasTransicoes().stream().filter(t -> t.acao() == AcaoProcesso.TENTAR_PAGAMENTO).count());
    }

    @Test
    void semProcessoEmPagamentoSolicitadoNaoChamaATesouraria() {
        rotinaComTesouraria(new OrdemPagamentoResponse("ACCEPTED", "778899", null)).executar();

        assertTrue(pedidos.isEmpty());
    }
}
