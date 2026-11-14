package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Padrão: Service Layer [PoEAA]. Aplica ao processo uma ação que só muda o estado, registra-a no
 * histórico e devolve o novo estado.
 * A alçada (R16, {@code processo.alcada}) é configuração: este caso de uso a lê e a entrega ao domínio como
 * {@link Dinheiro}; quem decide entre aprovado e aguardando alçada é a tabela do fluxo.
 * Ação com dado próprio (ex.: solicitar pagamento, que traz a identificação da solicitação) tem o seu próprio use case.
 */
@ApplicationScoped
public class ExecutarAcaoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final Dinheiro alcada;

    public ExecutarAcaoProcessoUseCase(ProcessoPagamentoRepository processos,
            @ConfigProperty(name = "processo.alcada") BigDecimal alcada) {
        this.processos = processos;
        this.alcada = Dinheiro.de(alcada);
    }

    @Transactional
    public EstadoProcesso executar(Long id, AcaoProcesso acao, String observacao) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        processo.executar(acao, Instant.now(), observacao, alcada);
        return processos.salvar(processo).estado();
    }
}
