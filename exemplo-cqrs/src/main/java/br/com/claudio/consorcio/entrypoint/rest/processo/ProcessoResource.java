package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.application.processo.AvancarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.CriarProcessoUseCase;
import br.com.claudio.consorcio.domain.processo.TipoProcesso;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

/** Criação do processo e um endpoint por transição de estado. */
@Path("/processos")
public class ProcessoResource {

    public record NovoProcesso(@NotNull Long cotaId, @NotNull TipoProcesso tipo) {
    }

    public record EstadoAtual(String estado) {
    }

    private final CriarProcessoUseCase criar;
    private final AvancarProcessoUseCase avancar;

    public ProcessoResource(CriarProcessoUseCase criar, AvancarProcessoUseCase avancar) {
        this.criar = criar;
        this.avancar = avancar;
    }

    @POST
    public Response criar(@Valid @NotNull NovoProcesso dados) {
        Long id = criar.criar(dados.cotaId(), dados.tipo());
        return Response.created(URI.create("/processos/" + id)).entity(new IdCriado(id)).build();
    }

    @POST
    @Path("/{id}/iniciar-analise")
    public EstadoAtual iniciarAnalise(Long id) {
        return new EstadoAtual(avancar.iniciarAnalise(id));
    }

    @POST
    @Path("/{id}/concluir-analise")
    public EstadoAtual concluirAnalise(Long id) {
        return new EstadoAtual(avancar.concluirAnalise(id));
    }

    @POST
    @Path("/{id}/concluir-vistoria")
    public EstadoAtual concluirVistoria(Long id) {
        return new EstadoAtual(avancar.concluirVistoria(id));
    }

    @POST
    @Path("/{id}/pagar")
    public EstadoAtual pagar(Long id) {
        return new EstadoAtual(avancar.pagar(id));
    }

    @POST
    @Path("/{id}/cancelar")
    public EstadoAtual cancelar(Long id) {
        return new EstadoAtual(avancar.cancelar(id));
    }
}
