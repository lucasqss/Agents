package br.com.claudio.consorcio.command.entrypoint;

import br.com.claudio.consorcio.command.application.AvancarProcessoUseCase;
import br.com.claudio.consorcio.command.application.CriarProcessoUseCase;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;

/** Criação do processo e um endpoint por transição de estado. */
@Path("/processos")
public class ProcessoResource {

    public record NovoProcesso(Long cotaId, TipoProcesso tipo) {
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
    public Response criar(NovoProcesso dados) {
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
