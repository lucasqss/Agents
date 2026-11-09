package br.com.claudio.consorcio.entrypoint.rest.cota;

import br.com.claudio.consorcio.application.cota.GerenciarCotaUseCase;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/cotas")
public class CotaResource {

    private final GerenciarCotaUseCase useCase;

    public CotaResource(GerenciarCotaUseCase useCase) {
        this.useCase = useCase;
    }

    @POST
    public Response criar(@Valid @NotNull CotaRequest dados) {
        Long id = useCase.criar(dados.paraDados());
        return Response.created(URI.create("/cotas/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull CotaRequest dados) {
        useCase.atualizar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        useCase.excluir(id);
    }
}
