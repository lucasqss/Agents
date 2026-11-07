package br.com.claudio.consorcio.canal.web.entrypoint;

import br.com.claudio.consorcio.canal.comum.erro.IdCriado;
import br.com.claudio.consorcio.command.application.GerenciarClienteUseCase;
import br.com.claudio.consorcio.query.application.ListarClientesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal web")
@Path("/web/v1/clientes")
public class ClienteResource {

    private final GerenciarClienteUseCase gerenciar;
    private final ListarClientesUseCase listar;

    public ClienteResource(GerenciarClienteUseCase gerenciar, ListarClientesUseCase listar) {
        this.gerenciar = gerenciar;
        this.listar = listar;
    }

    @GET
    public List<ClienteWebResponse> listar() {
        return WebAssembler.clientes(listar.executar());
    }

    @POST
    public Response criar(@Valid @NotNull ClienteRequest dados) {
        Long id = gerenciar.criar(dados.paraDados());
        return Response.created(URI.create("/web/v1/clientes/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull ClienteRequest dados) {
        gerenciar.atualizar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        gerenciar.excluir(id);
    }
}
