package br.com.claudio.pagamento.application.processo;

import br.com.claudio.pagamento.application.NaoEncontradoException;
import br.com.claudio.pagamento.persistence.processo.ProcessoQueryRepository;
import br.com.claudio.pagamento.persistence.processo.ProcessoResumo;
import jakarta.enterprise.context.ApplicationScoped;

/** Leitura de tela: o processo como está gravado, sem passar pelo domínio. */
@ApplicationScoped
public class ConsultarProcessoUseCase {

    private final ProcessoQueryRepository consultas;

    public ConsultarProcessoUseCase(ProcessoQueryRepository consultas) {
        this.consultas = consultas;
    }

    public ProcessoResumo executar(Long id) {
        return consultas.detalhar(id).orElseThrow(() -> new NaoEncontradoException("Processo", id));
    }
}
