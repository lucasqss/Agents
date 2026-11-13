package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.application.Pagina;
import br.com.claudio.consorcio.application.processo.ListarAcoesDisponiveisUseCase;
import br.com.claudio.consorcio.application.processo.ListarFavorecidosProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ListarHistoricoProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ListarProcessosUseCase;
import br.com.claudio.consorcio.persistence.processo.ProcessoFavorecidoResumo;
import br.com.claudio.consorcio.persistence.processo.ProcessoResumo;
import br.com.claudio.consorcio.persistence.processo.ProcessoTransicaoResumo;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import java.util.List;
import java.util.Locale;

@Path("/processos")
public class ProcessoQueryResource {

    /** Ações no mesmo formato do caminho dos endpoints: "concluir-analise" é POST /processos/{id}/concluir-analise. */
    public record AcoesDisponiveis(List<String> acoes) {
    }

    private final ListarProcessosUseCase listar;
    private final ListarAcoesDisponiveisUseCase listarAcoes;
    private final ListarHistoricoProcessoUseCase listarHistorico;
    private final ListarFavorecidosProcessoUseCase listarFavorecidos;

    public ProcessoQueryResource(ListarProcessosUseCase listar, ListarAcoesDisponiveisUseCase listarAcoes,
            ListarHistoricoProcessoUseCase listarHistorico,
            ListarFavorecidosProcessoUseCase listarFavorecidos) {
        this.listar = listar;
        this.listarAcoes = listarAcoes;
        this.listarHistorico = listarHistorico;
        this.listarFavorecidos = listarFavorecidos;
    }

    @GET
    @Path("/{id}/acoes")
    public AcoesDisponiveis acoes(Long id) {
        return new AcoesDisponiveis(listarAcoes.executar(id).stream()
                .map(acao -> acao.name().toLowerCase(Locale.ROOT).replace('_', '-'))
                .toList());
    }

    @GET
    @Path("/{id}/historico")
    public List<ProcessoTransicaoResumo> historico(Long id) {
        return listarHistorico.executar(id)
                .orElseThrow(() -> new NotFoundException("Processo " + id + " não encontrado"));
    }

    @GET
    @Path("/{id}/favorecidos")
    public List<ProcessoFavorecidoResumo> favorecidos(Long id) {
        return listarFavorecidos.executar(id)
                .orElseThrow(() -> new NotFoundException("Processo " + id + " não encontrado"));
    }

    @GET
    public Pagina<ProcessoResumo> listar(
            @QueryParam("tipo") String tipo,
            @QueryParam("estado") String estado,
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return listar.executar(tipo, estado, pagina, tamanho);
    }
}
