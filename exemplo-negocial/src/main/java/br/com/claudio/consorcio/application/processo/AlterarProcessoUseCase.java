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
 * Padrão: Service Layer [PoEAA]. O cliente troca o valor e os favorecidos do processo em aberto (R24); vale na hora.
 * O valor novo é conferido contra o crédito da cota como na abertura (R13). Bloqueia a cota e depois o processo,
 * nesta ordem, que é a de todo caso de uso que bloqueia os dois: assim não há impasse, e duas alterações
 * simultâneas do mesmo processo (ou uma alteração e um pagamento) ficam em fila e a segunda relê o processo.
 */
@ApplicationScoped
public class AlterarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;

    public AlterarProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas) {
        this.processos = processos;
        this.cotas = cotas;
    }

    @Transactional
    public EstadoProcesso executar(Long id, Autor autor, BigDecimal valor, List<DadosFavorecido> favorecidos) {
        Dinheiro novoValor = Dinheiro.de(valor);
        List<Favorecido> novosFavorecidos = favorecidos.stream().map(DadosFavorecido::paraFavorecido).toList();
        // O id da cota não muda; ler o processo sem bloqueio só serve para achar a cota, que vem primeiro na ordem.
        Long cotaId = processos.buscarPorId(id).orElseThrow(() -> new NaoEncontradoException("Processo", id)).cotaId();
        Cota cota = cotas.buscarPorIdComBloqueio(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        // R18: só o cliente da cota altera; conferido com o dado da cota, depois do 404 e antes da situação (409).
        cota.exigirDoCliente(autor.clienteId());
        ProcessoPagamento processo = processos.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.alterar(autor, novoValor, novosFavorecidos, Instant.now());
        // Nada foi gravado: se o crédito não cobre o valor, a transação termina sem efeito.
        cota.exigirCreditoPara(novoValor, processos.somarValorPagoPorCota(cotaId));
        return processos.salvar(processo).estado();
    }
}
