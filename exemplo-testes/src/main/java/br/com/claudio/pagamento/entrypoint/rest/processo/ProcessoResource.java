package br.com.claudio.pagamento.entrypoint.rest.processo;

import br.com.claudio.pagamento.application.processo.AbrirProcessoUseCase;
import br.com.claudio.pagamento.application.processo.ConsultarProcessoUseCase;
import br.com.claudio.pagamento.application.processo.ExecutarAcaoProcessoUseCase;
import br.com.claudio.pagamento.application.processo.ListarAcoesDisponiveisUseCase;
import br.com.claudio.pagamento.application.processo.ListarHistoricoProcessoUseCase;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import br.com.claudio.pagamento.entrypoint.IdCriado;
import br.com.claudio.pagamento.persistence.processo.ProcessoResumo;
import br.com.claudio.pagamento.persistence.processo.ProcessoTransicaoResumo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.jspecify.annotations.Nullable;

/**
 * Processo de pagamento: abrir, consultar e executar uma ação. Todas as ações passam pelo mesmo endpoint e pelo mesmo use
 * case: quem diz se a ação vale é a tabela do fluxo do tipo, no domínio. Quem pede a ação vem do cabeçalho
 * {@value #USUARIO} (num serviço real, do token de identidade) e fica no histórico.
 */
@Path("/processos")
public class ProcessoResource {

    static final String USUARIO = "X-Usuario";

    public record NovoProcesso(@NotNull TipoProcesso tipo,
            @NotBlank @Size(max = ProcessoPagamento.TAMANHO_MAXIMO_DA_COTA) String numeroCota,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor) {
    }

    /** Corpo opcional das ações: o motivo, obrigatório só nas ações que o exigem (regra do domínio, R5). */
    public record Motivo(@Size(max = Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) @Nullable String motivo) {

        static @Nullable String texto(@Nullable Motivo corpo) {
            return corpo == null ? null : corpo.motivo();
        }
    }

    public record EstadoAtual(EstadoProcesso estado) {
    }

    private final AbrirProcessoUseCase abrir;
    private final ExecutarAcaoProcessoUseCase executarAcao;
    private final ConsultarProcessoUseCase consultar;
    private final ListarAcoesDisponiveisUseCase listarAcoes;
    private final ListarHistoricoProcessoUseCase listarHistorico;

    public ProcessoResource(AbrirProcessoUseCase abrir, ExecutarAcaoProcessoUseCase executarAcao,
            ConsultarProcessoUseCase consultar, ListarAcoesDisponiveisUseCase listarAcoes,
            ListarHistoricoProcessoUseCase listarHistorico) {
        this.abrir = abrir;
        this.executarAcao = executarAcao;
        this.consultar = consultar;
        this.listarAcoes = listarAcoes;
        this.listarHistorico = listarHistorico;
    }

    @POST
    @APIResponse(responseCode = "201", description = "Processo aberto",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdCriado.class)))
    public Response abrir(@Valid @NotNull NovoProcesso dados) {
        Long id = abrir.executar(dados.tipo(), dados.numeroCota(), dados.valor());
        return Response.created(URI.create("/processos/" + id)).entity(new IdCriado(id)).build();
    }

    @GET
    @Path("/{id}")
    public ProcessoResumo consultar(@PathParam("id") Long id) {
        return consultar.executar(id);
    }

    /** As ações que o processo aceita agora, em ordem alfabética. */
    @GET
    @Path("/{id}/acoes")
    public List<AcaoProcesso> acoes(@PathParam("id") Long id) {
        return listarAcoes.executar(id).stream().sorted(Comparator.comparing(Enum::name)).toList();
    }

    @POST
    @Path("/{id}/acoes/{acao}")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual executar(@PathParam("id") Long id, @PathParam("acao") AcaoProcesso acao,
            @HeaderParam(USUARIO) @NotBlank @Size(max = Transicao.TAMANHO_MAXIMO_DO_AUTOR) String usuario,
            @RequestBody(required = false) @Valid @Nullable Motivo corpo) {
        return new EstadoAtual(executarAcao.executar(id, acao, usuario, Motivo.texto(corpo)));
    }

    @GET
    @Path("/{id}/historico")
    public List<ProcessoTransicaoResumo> historico(@PathParam("id") Long id) {
        return listarHistorico.executar(id);
    }
}
