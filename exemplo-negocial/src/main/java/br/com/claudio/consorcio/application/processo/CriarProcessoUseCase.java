package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import br.com.claudio.consorcio.domain.processo.Documento;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

/** Padrão: Service Layer [PoEAA]. Cria um processo novo, dividido entre os favorecidos, para uma cota sem processo em andamento e com crédito para o valor. */
@ApplicationScoped
public class CriarProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;

    public CriarProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas) {
        this.processos = processos;
        this.cotas = cotas;
    }

    @Transactional
    public Long executar(Long cotaId, TipoProcesso tipo, BigDecimal valor, List<DadosFavorecido> favorecidos) {
        // As regras dos favorecidos (R15, R29) são conferidas ao montar o agregado, antes de buscar a cota.
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, cotaId, Dinheiro.de(valor), favorecidos.stream()
                .map(f -> new Favorecido(f.nome(), Documento.de(f.documento()), f.papel(), Dinheiro.de(f.valor())))
                .toList());
        // O bloqueio coloca em fila as aberturas da mesma cota; sem ele, duas passariam juntas pelas verificações abaixo,
        // inclusive a do crédito, cuja soma dos pagos é lida dentro desta mesma transação.
        Cota cota = cotas.buscarPorIdComBloqueio(cotaId).orElseThrow(() -> new NaoEncontradoException("Cota", cotaId));
        if (processos.existeAtivoPorCota(cotaId)) {
            throw new ConflitoException("Cota " + cotaId + " já possui um processo em andamento");
        }
        cota.exigirCreditoPara(processo.valor(), processos.somarValorPagoPorCota(cotaId));
        return processos.salvar(processo).id();
    }
}
