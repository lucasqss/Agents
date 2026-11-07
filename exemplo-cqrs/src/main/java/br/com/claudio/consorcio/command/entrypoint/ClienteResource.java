package br.com.claudio.consorcio.command.entrypoint;

import br.com.claudio.consorcio.command.application.DadosCliente;
import br.com.claudio.consorcio.command.application.GerenciarClienteUseCase;
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
    public Response criar(DadosCliente dados) {
        Long id = useCase.criar(dados);
        return Response.created(URI.create("/clientes/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, DadosCliente dados) {
        useCase.atualizar(id, dados);
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        useCase.excluir(id);
    }
}
