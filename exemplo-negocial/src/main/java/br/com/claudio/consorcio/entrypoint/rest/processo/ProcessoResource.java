package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.application.processo.CriarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ExecutarAcaoProcessoUseCase;
import br.com.claudio.consorcio.application.processo.PagarProcessoUseCase;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

/**
 * Criação do processo e um endpoint por ação. Os endpoints são explícitos (aparecem um a um no OpenAPI),
 * mas as ações que só mudam o estado passam todas pelo mesmo use case.
 */
@Path("/processos")
public class ProcessoResource {

    public record NovoProcesso(@NotNull Long cotaId, @NotNull TipoProcesso tipo) {
    }

    /** Corpo opcional das ações: um texto livre que fica no histórico. */
    public record Observacao(@Size(max = Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) String observacao) {

        static String texto(Observacao corpo) {
            return corpo == null ? null : corpo.observacao();
        }
    }

    public record EstadoAtual(String estado) {

        static EstadoAtual de(EstadoProcesso estado) {
            return new EstadoAtual(estado.name());
        }
    }

    private final CriarProcessoUseCase criar;
    private final ExecutarAcaoProcessoUseCase executarAcao;
    private final PagarProcessoUseCase pagar;

    public ProcessoResource(CriarProcessoUseCase criar, ExecutarAcaoProcessoUseCase executarAcao,
            PagarProcessoUseCase pagar) {
        this.criar = criar;
        this.executarAcao = executarAcao;
        this.pagar = pagar;
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
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual iniciarAnalise(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.INICIAR_ANALISE, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/concluir-analise")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual concluirAnalise(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.CONCLUIR_ANALISE, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/concluir-vistoria")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual concluirVistoria(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.CONCLUIR_VISTORIA, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/devolver-para-analise")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual devolverParaAnalise(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.DEVOLVER_PARA_ANALISE, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/pagar")
    public EstadoAtual pagar(Long id) {
        return EstadoAtual.de(pagar.executar(id));
    }

    @POST
    @Path("/{id}/cancelar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual cancelar(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.CANCELAR, Observacao.texto(corpo)));
    }
}
