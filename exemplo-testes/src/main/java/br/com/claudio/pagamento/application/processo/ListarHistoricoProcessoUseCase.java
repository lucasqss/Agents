package br.com.claudio.pagamento.application.processo;

import br.com.claudio.pagamento.application.NaoEncontradoException;
import br.com.claudio.pagamento.persistence.processo.ProcessoQueryRepository;
import br.com.claudio.pagamento.persistence.processo.ProcessoTransicaoResumo;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

/** R8: as transições do processo, da mais antiga para a mais recente. */
@ApplicationScoped
public class ListarHistoricoProcessoUseCase {

    private final ProcessoQueryRepository consultas;

    public ListarHistoricoProcessoUseCase(ProcessoQueryRepository consultas) {
        this.consultas = consultas;
    }

    public List<ProcessoTransicaoResumo> executar(Long id) {
        if (consultas.contar(id) == 0) {
            throw new NaoEncontradoException("Processo", id);
        }
        return consultas.historico(id);
    }
}
