package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.application.processo.CancelarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ConcluirAnaliseProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ConcluirVistoriaProcessoUseCase;
import br.com.claudio.consorcio.application.processo.CriarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.IniciarAnaliseProcessoUseCase;
import br.com.claudio.consorcio.application.processo.PagarProcessoUseCase;
import br.com.claudio.consorcio.domain.processo.TipoProcesso;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/** Criação do processo e um endpoint por transição de estado. */
@Path("/processos")
public class ProcessoResource {

    public record NovoProcesso(@NotNull Long cotaId, @NotNull TipoProcesso tipo) {
    }

    public record EstadoAtual(String estado) {
    }

    private final CriarProcessoUseCase criar;
    private final IniciarAnaliseProcessoUseCase iniciarAnalise;
    private final ConcluirAnaliseProcessoUseCase concluirAnalise;
    private final ConcluirVistoriaProcessoUseCase concluirVistoria;
    private final PagarProcessoUseCase pagar;
    private final CancelarProcessoUseCase cancelar;

    public ProcessoResource(CriarProcessoUseCase criar,
            IniciarAnaliseProcessoUseCase iniciarAnalise,
            ConcluirAnaliseProcessoUseCase concluirAnalise,
            ConcluirVistoriaProcessoUseCase concluirVistoria,
            PagarProcessoUseCase pagar,
            CancelarProcessoUseCase cancelar) {
        this.criar = criar;
        this.iniciarAnalise = iniciarAnalise;
        this.concluirAnalise = concluirAnalise;
        this.concluirVistoria = concluirVistoria;
        this.pagar = pagar;
        this.cancelar = cancelar;
    }

    @POST
    @APIResponse(responseCode = "201", description = "Processo criado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdCriado.class)))
    public Response criar(@Valid @NotNull NovoProcesso dados) {
        Long id = criar.executar(dados.cotaId(), dados.tipo());
        return Response.created(URI.create("/processos/" + id)).entity(new IdCriado(id)).build();
    }

    @POST
    @Path("/{id}/iniciar-analise")
    public EstadoAtual iniciarAnalise(Long id) {
        return new EstadoAtual(iniciarAnalise.executar(id));
    }

    @POST
    @Path("/{id}/concluir-analise")
    public EstadoAtual concluirAnalise(Long id) {
        return new EstadoAtual(concluirAnalise.executar(id));
    }

    @POST
    @Path("/{id}/concluir-vistoria")
    public EstadoAtual concluirVistoria(Long id) {
        return new EstadoAtual(concluirVistoria.executar(id));
    }

    @POST
    @Path("/{id}/pagar")
    public EstadoAtual pagar(Long id) {
        return new EstadoAtual(pagar.executar(id));
    }

    @POST
    @Path("/{id}/cancelar")
    public EstadoAtual cancelar(Long id) {
        return new EstadoAtual(cancelar.executar(id));
    }
}
