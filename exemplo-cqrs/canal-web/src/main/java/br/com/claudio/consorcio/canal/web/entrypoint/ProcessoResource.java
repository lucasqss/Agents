package br.com.claudio.consorcio.canal.web.entrypoint;

import br.com.claudio.consorcio.canal.comum.erro.IdCriado;
import br.com.claudio.consorcio.canal.comum.seguranca.SolicitanteDaRequisicao;
import br.com.claudio.consorcio.command.application.AvancarProcessoUseCase;
import br.com.claudio.consorcio.command.application.CriarProcessoUseCase;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import br.com.claudio.consorcio.query.application.ListarProcessosUseCase;
import br.com.claudio.consorcio.query.application.Pagina;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

/** Processos no web: lista paginada, criação e um endpoint por transição de estado. */
@Tag(name = "Canal web")
@Path("/web/v1/processos")
public class ProcessoResource {

    public record NovoProcesso(@NotNull Long cotaId, @NotNull TipoProcesso tipo) {
    }

    public record EstadoAtual(String estado) {
    }

    private final CriarProcessoUseCase criar;
    private final AvancarProcessoUseCase avancar;
    private final ListarProcessosUseCase listar;
    private final SolicitanteDaRequisicao solicitante;

    public ProcessoResource(CriarProcessoUseCase criar, AvancarProcessoUseCase avancar, ListarProcessosUseCase listar,
            SolicitanteDaRequisicao solicitante) {
        this.criar = criar;
        this.avancar = avancar;
        this.listar = listar;
        this.solicitante = solicitante;
    }

    @GET
    public Pagina<ProcessoWebResponse> listar(
            @QueryParam("tipo") String tipo,
            @QueryParam("estado") String estado,
            @QueryParam("pagina") @DefaultValue("1") int pagina,
            @QueryParam("tamanho") @DefaultValue("20") int tamanho) {
        return WebAssembler.processos(listar.executar(tipo, estado, pagina, tamanho));
    }

    @POST
    public Response criar(@Valid @NotNull NovoProcesso dados) {
        Long id = criar.criar(solicitante.atual(), dados.cotaId(), dados.tipo());
        return Response.created(URI.create("/web/v1/processos/" + id)).entity(new IdCriado(id)).build();
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
