package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.ResultadoPagamento;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.time.Instant;

/**
 * Padrão: Service Layer [PoEAA]. Último passo da saga de pagamento (ADR 002): em transação curta, relê o processo com
 * bloqueio e aplica o resultado que a tesouraria deu, só se ele ainda espera o pagamento daquela solicitação. Se outra
 * execução da rotina já aplicou o resultado, nada muda e devolve falso. Não chama a tesouraria: a chamada foi feita antes,
 * fora de transação.
 */
@ApplicationScoped
public class AplicarResultadoPagamentoUseCase {

    private final ProcessoPagamentoRepository processos;

    public AplicarResultadoPagamentoUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    @Transactional
    public boolean executar(Long id, IdentificacaoSolicitacao identificacao, ResultadoPagamento resultado) {
        ProcessoPagamento processo = processos.buscarPorIdComBloqueio(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id));
        if (!processo.aplicarResultado(identificacao, resultado, Instant.now())) {
            return false;
        }
        processos.salvar(processo);
        return true;
    }
}
