package br.com.claudio.consorcio.entrypoint.rest.cota;

import br.com.claudio.consorcio.application.cota.AtualizarCotaUseCase;
import br.com.claudio.consorcio.application.cota.CriarCotaUseCase;
import br.com.claudio.consorcio.application.cota.ExcluirCotaUseCase;
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

    private final CriarCotaUseCase criar;
    private final AtualizarCotaUseCase atualizar;
    private final ExcluirCotaUseCase excluir;

    public CotaResource(CriarCotaUseCase criar, AtualizarCotaUseCase atualizar, ExcluirCotaUseCase excluir) {
        this.criar = criar;
        this.atualizar = atualizar;
        this.excluir = excluir;
    }

    @POST
    public Response criar(@Valid @NotNull CotaRequest dados) {
        Long id = criar.executar(dados.paraDados());
        return Response.created(URI.create("/cotas/" + id)).entity(new IdCriado(id)).build();
    }

    @PUT
    @Path("/{id}")
    public void atualizar(Long id, @Valid @NotNull CotaRequest dados) {
        atualizar.executar(id, dados.paraDados());
    }

    @DELETE
    @Path("/{id}")
    public void excluir(Long id) {
        excluir.executar(id);
    }
}
