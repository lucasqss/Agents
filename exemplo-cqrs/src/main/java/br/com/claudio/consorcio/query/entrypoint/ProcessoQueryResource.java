package br.com.claudio.consorcio.query.entrypoint;

import br.com.claudio.consorcio.query.application.ListarProcessosUseCase;
import br.com.claudio.consorcio.query.application.Pagina;
import br.com.claudio.consorcio.query.persistence.ProcessoResumo;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;

@Path("/processos")
public class ProcessoQueryResource {

    private final ListarProcessosUseCase listar;

    public ProcessoQueryResource(ListarProcessosUseCase listar) {
        this.listar = listar;
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
