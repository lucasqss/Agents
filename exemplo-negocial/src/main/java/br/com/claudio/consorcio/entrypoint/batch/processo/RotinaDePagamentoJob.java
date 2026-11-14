package br.com.claudio.consorcio.entrypoint.batch.processo;

import br.com.claudio.consorcio.application.processo.ExecutarRotinaDePagamentoUseCase;
import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Rotina de pagamento (R20): a cada intervalo ({@code pagamento.rotina.intervalo}, 5 minutos por padrão; {@code off}
 * desliga), pede à tesouraria o pagamento dos processos em Pagamento solicitado. Só chama o caso de uso. Não há endpoint
 * para dispará-la. Uma rodada que ainda não terminou impede a seguinte nesta instância; entre instâncias, quem protege é
 * o bloqueio do processo na hora de gravar o resultado.
 */
@ApplicationScoped
public class RotinaDePagamentoJob {

    private final ExecutarRotinaDePagamentoUseCase rotina;

    public RotinaDePagamentoJob(ExecutarRotinaDePagamentoUseCase rotina) {
        this.rotina = rotina;
    }

    @Scheduled(identity = "rotina-de-pagamento", every = "{pagamento.rotina.intervalo}",
            concurrentExecution = ConcurrentExecution.SKIP)
    void executar() {
        rotina.executar();
    }
}
