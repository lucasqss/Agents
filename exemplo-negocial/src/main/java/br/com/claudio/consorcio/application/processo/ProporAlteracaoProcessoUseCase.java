package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import br.com.claudio.consorcio.domain.processo.Autor;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Padrão: Service Layer [PoEAA]. O backoffice propõe novo valor e novos favorecidos a um processo em análise, com
 * motivo (R25); o processo passa a aguardar o cliente e os dados vigentes não mudam. O valor proposto é conferido
 * contra o crédito da cota agora (R13); reconferir ao concordar é dúvida aberta (D21). Bloqueia a cota e depois o
 * processo, a mesma ordem de {@link AlterarProcessoUseCase}.
 */
@ApplicationScoped
public class ProporAlteracaoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;

    public ProporAlteracaoProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas) {
        this.processos = processos;
        this.cotas = cotas;
    }

    @Transactional
    public EstadoProcesso executar(Long id, Autor autor, BigDecimal valor, List<DadosFavorecido> favorecidos, String motivo) {
        Dinheiro novoValor = Dinheiro.de(valor);
        List<Favorecido> novosFavorecidos = favorecidos.stream().map(DadosFavorecido::paraFavorecido).toList();
        Long cotaId = processos.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Processo", id)).cotaId();
        Cota cota = cotas.buscarPorIdComBloqueio(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        ProcessoPagamento processo = processos.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.proporAlteracao(autor, novoValor, novosFavorecidos, motivo, Instant.now());
        cota.exigirCreditoPara(novoValor, processos.somarValorPagoPorCota(cotaId));
        return processos.salvar(processo).estado();
    }
}
