package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import br.com.claudio.consorcio.domain.processo.Autor;
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
 * Quem pede ({@link Autor}) é conferido: o papel e a regra da segunda pessoa (R17, R18) pela linha da tabela do fluxo,
 * na raiz; e, quando a linha é do cliente (concordar, discordar), que o cliente do autor é o dono da cota, aqui, com o
 * dado da cota. O processo inexistente é 404 antes de qualquer recusa de pessoa, e o dono é conferido antes de a
 * situação ser (409).
 * Ação com dado próprio (ex.: solicitar pagamento, que traz a identificação da solicitação) tem o seu próprio use case.
 */
@ApplicationScoped
public class ExecutarAcaoProcessoUseCase {

    private final ProcessoPagamentoRepository processos;
    private final CotaRepository cotas;
    private final Dinheiro alcada;

    public ExecutarAcaoProcessoUseCase(ProcessoPagamentoRepository processos, CotaRepository cotas,
            @ConfigProperty(name = "processo.alcada") BigDecimal alcada) {
        this.processos = processos;
        this.cotas = cotas;
        this.alcada = Dinheiro.de(alcada);
    }

    @Transactional
    public EstadoProcesso executar(Long id, Autor autor, AcaoProcesso acao, String observacao) {
        ProcessoPagamento processo = processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        if (processo.exigeClienteDaCota(acao)) {
            Cota cota = cotas.buscarPorId(processo.cotaId())
                    .orElseThrow(() -> new NaoEncontradoException("Cota", processo.cotaId()));
            cota.exigirDoCliente(autor.clienteId());
        }
        processo.executar(autor, acao, Instant.now(), observacao, alcada);
        return processos.salvar(processo).estado();
    }
}
