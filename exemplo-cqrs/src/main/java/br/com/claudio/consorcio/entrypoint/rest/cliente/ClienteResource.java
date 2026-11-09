package br.com.claudio.consorcio.entrypoint.rest.cliente;

import br.com.claudio.consorcio.application.cliente.GerenciarClienteUseCase;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

@Path("/clientes")
public class ClienteResource {

    private final GerenciarClienteUseCase useCase;

    public ClienteResource(GerenciarClienteUseCase useCase) {
        this.useCase = useCase;
    }

    @POST
    public Response criar(@Valid @NotNull ClienteRequest dados) {
        Long id = useCase.criar(dados.paraDados());
        return Response.created(URI.create("/clientes/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull ClienteRequest dados) {
        useCase.atualizar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        useCase.excluir(id);
    }
}
