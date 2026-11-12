package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Set;

/**
 * Ações que o processo aceita agora. Quem responde é o domínio (o fluxo do tipo), para que nenhum front
 * precise repetir a tabela de transições para decidir quais botões mostrar.
 */
@ApplicationScoped
public class ListarAcoesDisponiveisUseCase {

    private final ProcessoPagamentoRepository processos;

    public ListarAcoesDisponiveisUseCase(ProcessoPagamentoRepository processos) {
        this.processos = processos;
    }

    public Set<AcaoProcesso> executar(Long id) {
        return processos.buscarPorId(id)
                .orElseThrow(() -> new NaoEncontradoException("Processo", id))
                .acoesDisponiveis();
    }
}
