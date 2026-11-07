package br.com.claudio.consorcio.canal.web.entrypoint;

import br.com.claudio.consorcio.canal.comum.erro.IdCriado;
import br.com.claudio.consorcio.command.application.usecase.GerenciarCotaUseCase;
import br.com.claudio.consorcio.query.application.usecase.DetalharCotaUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal web")
@Path("/web/v1/cotas")
public class CotaResource {

    private final GerenciarCotaUseCase gerenciar;
    private final DetalharCotaUseCase detalhar;

    public CotaResource(GerenciarCotaUseCase gerenciar, DetalharCotaUseCase detalhar) {
        this.gerenciar = gerenciar;
        this.detalhar = detalhar;
    }

    @GET
    @Path("/{id}")
    public CotaWebResponse detalhar(Long id) {
        return detalhar.executar(id).map(WebAssembler::cota)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }

    @POST
    public Response criar(@Valid @NotNull CotaRequest dados) {
        Long id = gerenciar.criar(dados.paraDados());
        return Response.created(URI.create("/web/v1/cotas/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull CotaRequest dados) {
        gerenciar.atualizar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        gerenciar.excluir(id);
    }
}
