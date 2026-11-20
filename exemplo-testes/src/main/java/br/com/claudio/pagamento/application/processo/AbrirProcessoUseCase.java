package br.com.claudio.pagamento.application.processo;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Objects;

/** Padrão: Service Layer [PoEAA]. Abre um processo novo, em Aberto (R1), e devolve o id gerado. */
@ApplicationScoped
public class AbrirProcessoUseCase {

    private final ProcessoPagamentoRepository processos;

    public AbrirProcessoUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    @Transactional
    public Long executar(TipoProcesso tipo, String numeroCota, BigDecimal valor) {
        ProcessoPagamento processo = ProcessoPagamento.novo(tipo, numeroCota, Dinheiro.de(valor));
        return Objects.requireNonNull(processos.salvar(processo).id(), "O repositório devolve o processo com id");
    }
}
