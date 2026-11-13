package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.AlteracaoConcorrenteException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.AlteracaoProposta;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import jakarta.data.exceptions.OptimisticLockingFailureException;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Padrão: Repository [PoEAA]; implementa a Separated Interface do domínio sobre o Jakarta Data. */
@ApplicationScoped
public class ProcessoPagamentoRepositoryAdapter implements ProcessoPagamentoRepository {

    private static final List<String> ESTADOS_FINAIS = Arrays.stream(EstadoProcesso.values())
            .filter(EstadoProcesso::finalizado)
            .map(EstadoProcesso::name)
            .toList();

    private final ProcessoPagamentoDataRepository dados;

    public ProcessoPagamentoRepositoryAdapter(ProcessoPagamentoDataRepository dados) {
        this.dados = dados;
    }

    @Override
    public ProcessoPagamento salvar(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = ProcessoPagamentoMapper.paraEntidade(processo);
        if (processo.id() == null) {
            dados.inserir(entity);
            for (ProcessoFavorecidoEntity favorecido : ProcessoPagamentoMapper.paraEntidades(entity.getId(),
                    processo.favorecidos())) {
                dados.inserirFavorecido(favorecido);
            }
        } else {
            // A versão é conferida primeiro: quem perdeu a disputa falha aqui, sem mexer nos filhos.
            atualizar(entity);
            trocarFavorecidosSeMudaram(entity.getId(), processo.favorecidos());
            trocarPropostaSeMudou(entity.getId(), processo.propostaPendente().orElse(null));
        }
        for (Transicao transicao : processo.novasTransicoes()) {
            dados.inserirTransicao(ProcessoPagamentoMapper.paraEntidade(entity.getId(), transicao));
        }
        return reconstituir(entity, false);
    }

    /**
     * Na leitura sem bloqueio, o processo e os filhos vêm de consultas separadas: se outra transação confirmar no meio,
     * o conjunto é incoerente e a recusa do domínio vira {@link AlteracaoConcorrenteException} (409). Com bloqueio, ou
     * depois de gravar, os dados são coerentes: recusa do domínio ali é dado inválido no banco (IllegalStateException).
     */
    private ProcessoPagamento reconstituir(ProcessoPagamentoEntity entity, boolean leituraSemBloqueio) {
        try {
            return ProcessoPagamentoMapper.paraDominio(entity, dados.buscarFavorecidos(entity.getId()),
                    lerProposta(entity.getId()));
        } catch (DominioException e) {
            if (leituraSemBloqueio) {
                AlteracaoConcorrenteException concorrente = new AlteracaoConcorrenteException("Processo", entity.getId());
                concorrente.initCause(e);
                throw concorrente;
            }
            throw new IllegalStateException("Valor desconhecido no banco: processo " + entity.getId() + " inválido: "
                    + e.getMessage(), e);
        }
    }

    /** Apaga e regrava os favorecidos vigentes quando o agregado trouxe outros; as restrições unique valem a cada gravação. */
    private void trocarFavorecidosSeMudaram(Long processoId, List<Favorecido> desejados) {
        List<Favorecido> gravados = ProcessoPagamentoMapper.favorecidosParaDominio(dados.buscarFavorecidos(processoId));
        if (gravados.equals(desejados)) {
            return;
        }
        dados.removerFavorecidos(processoId);
        for (ProcessoFavorecidoEntity favorecido : ProcessoPagamentoMapper.paraEntidades(processoId, desejados)) {
            dados.inserirFavorecido(favorecido);
        }
    }

    /** Grava a proposta pendente e apaga a que deixou de valer (concordou, discordou ou cancelou). */
    private void trocarPropostaSeMudou(Long processoId, AlteracaoProposta desejada) {
        AlteracaoProposta gravada = lerProposta(processoId);
        if (Objects.equals(gravada, desejada)) {
            return;
        }
        if (gravada != null) {
            dados.removerFavorecidosPropostos(processoId);
            dados.removerProposta(processoId);
        }
        if (desejada != null) {
            dados.inserirProposta(ProcessoPagamentoMapper.propostaParaEntidade(processoId, desejada));
            for (ProcessoPropostaFavorecidoEntity favorecido
                    : ProcessoPagamentoMapper.favorecidosDaPropostaParaEntidades(processoId, desejada)) {
                dados.inserirFavorecidoProposto(favorecido);
            }
        }
    }

    private AlteracaoProposta lerProposta(Long processoId) {
        return dados.buscarProposta(processoId)
                .map(p -> ProcessoPagamentoMapper.propostaParaDominio(p, dados.buscarFavorecidosPropostos(processoId)))
                .orElse(null);
    }

    private void atualizar(ProcessoPagamentoEntity entity) {
        try {
            dados.atualizar(entity);
        } catch (OptimisticLockingFailureException e) {
            throw new AlteracaoConcorrenteException("Processo", entity.getId());
        }
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(e -> reconstituir(e, true));
    }

    @Override
    public Optional<ProcessoPagamento> buscarPorIdComBloqueio(Long id) {
        return dados.buscarPorIdComBloqueio(id).map(e -> reconstituir(e, false));
    }

    @Override
    public boolean existePorCota(Long cotaId) {
        return dados.contarPorCota(cotaId) > 0;
    }

    @Override
    public boolean existeAtivoPorCota(Long cotaId) {
        return dados.contarAtivosPorCota(cotaId, ESTADOS_FINAIS) > 0;
    }

    @Override
    public Dinheiro somarValorPagoPorCota(Long cotaId) {
        BigDecimal soma = dados.somarValorPorCotaEEstado(cotaId, EstadoProcesso.PAGO.name());
        return soma == null ? Dinheiro.ZERO : Dinheiro.de(soma);
    }
}
