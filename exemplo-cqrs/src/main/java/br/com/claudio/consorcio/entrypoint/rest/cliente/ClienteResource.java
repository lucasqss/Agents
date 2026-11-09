package br.com.claudio.consorcio.entrypoint.rest.cliente;

import br.com.claudio.consorcio.application.cliente.AtualizarClienteUseCase;
import br.com.claudio.consorcio.application.cliente.CriarClienteUseCase;
import br.com.claudio.consorcio.application.cliente.ExcluirClienteUseCase;
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

    private final CriarClienteUseCase criar;
    private final AtualizarClienteUseCase atualizar;
    private final ExcluirClienteUseCase excluir;

    public ClienteResource(CriarClienteUseCase criar, AtualizarClienteUseCase atualizar, ExcluirClienteUseCase excluir) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.excluir = excluir;
    }

    @POST
    public Response criar(@Valid @NotNull ClienteRequest dados) {
        Long id = criar.executar(dados.paraDados());
        return Response.created(URI.create("/clientes/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull ClienteRequest dados) {
        atualizar.executar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        excluir.executar(id);
    }
}
