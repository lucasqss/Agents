package br.com.claudio.pagamento.application.processo;

import br.com.claudio.pagamento.application.NaoEncontradoException;
import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Clock;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jspecify.annotations.Nullable;

/**
 * Padrão: Service Layer [PoEAA]. Aplica ao processo uma ação, registra-a no histórico e devolve o novo estado.
 * A alçada (R4, {@code processo.alcada}) é configuração: este caso de uso a lê e a entrega ao domínio como
 * {@link Dinheiro}; quem decide entre Aprovado e Aguardando alçada é a tabela do fluxo.
 */
@ApplicationScoped
public class ExecutarAcaoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final Dinheiro alcada;
    private final Clock relogio;

    public ExecutarAcaoProcessoUseCase(ProcessoPagamentoRepository processos,
            @ConfigProperty(name = "processo.alcada") BigDecimal alcada, Clock relogio) {
        this.processos = processos;
        this.alcada = Dinheiro.de(alcada);
        this.relogio = relogio;
    }

    @Transactional
    public EstadoProcesso executar(Long id, AcaoProcesso acao, String autor, @Nullable String motivo) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.executar(acao, autor, relogio.instant(), motivo, alcada);
        return processos.salvar(processo).estado();
    }
}
