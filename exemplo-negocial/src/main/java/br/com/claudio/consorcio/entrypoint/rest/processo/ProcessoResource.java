package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.application.processo.AlterarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.CriarProcessoUseCase;
import br.com.claudio.consorcio.application.processo.DadosFavorecido;
import br.com.claudio.consorcio.application.processo.ExecutarAcaoProcessoUseCase;
import br.com.claudio.consorcio.application.processo.ProporAlteracaoProcessoUseCase;
import br.com.claudio.consorcio.application.processo.SolicitarPagamentoProcessoUseCase;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.entrypoint.IdCriado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
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

    public record NovoProcesso(@NotNull Long cotaId, @NotNull TipoProcesso tipo,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
            @NotEmpty List<@NotNull @Valid NovoFavorecido> favorecidos) {

        List<DadosFavorecido> dadosFavorecidos() {
            return NovoFavorecido.dados(favorecidos);
        }
    }

    /** Novo valor e novos favorecidos que o cliente informa para o processo em aberto (R24). */
    public record Alteracao(@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
            @NotEmpty List<@NotNull @Valid NovoFavorecido> favorecidos) {
    }

    /** Alteração que o backoffice propõe, com o motivo que o cliente verá ao decidir (R25). */
    public record PropostaAlteracao(@NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor,
            @NotEmpty List<@NotNull @Valid NovoFavorecido> favorecidos,
            @NotBlank @Size(max = Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) String motivo) {
    }

    /** Um favorecido do pedido; só o formato é conferido aqui, a soma e a repetição são regras do domínio. */
    public record NovoFavorecido(@NotBlank @Size(max = 120) String nome,
            @NotBlank @Pattern(regexp = "\\d{11}|\\d{14}", message = "deve ter 11 (CPF) ou 14 (CNPJ) dígitos") String documento,
            @NotBlank @Size(max = 50) String papel,
            @NotNull @Positive @Digits(integer = 13, fraction = 2) BigDecimal valor) {

        static List<DadosFavorecido> dados(List<NovoFavorecido> favorecidos) {
            return favorecidos.stream()
                    .map(f -> new DadosFavorecido(f.nome(), f.documento(), f.papel(), f.valor()))
                    .toList();
        }
    }

    /** Corpo opcional das ações: um texto livre que fica no histórico. */
    public record Observacao(@Size(max = Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) String observacao) {

        static String texto(Observacao corpo) {
            return corpo == null ? null : corpo.observacao();
        }
    }

    /** Corpo obrigatório das ações que exigem motivo: o formato é conferido aqui, a regra é do domínio. */
    public record Motivo(@NotBlank @Size(max = Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO) String motivo) {
    }

    public record EstadoAtual(String estado) {

        static EstadoAtual de(EstadoProcesso estado) {
            return new EstadoAtual(estado.name());
        }
    }

    private final CriarProcessoUseCase criar;
    private final ExecutarAcaoProcessoUseCase executarAcao;
    private final SolicitarPagamentoProcessoUseCase solicitarPagamento;
    private final AlterarProcessoUseCase alterar;
    private final ProporAlteracaoProcessoUseCase proporAlteracao;

    public ProcessoResource(CriarProcessoUseCase criar, ExecutarAcaoProcessoUseCase executarAcao,
            SolicitarPagamentoProcessoUseCase solicitarPagamento, AlterarProcessoUseCase alterar, ProporAlteracaoProcessoUseCase proporAlteracao) {
        this.criar = criar;
        this.executarAcao = executarAcao;
        this.solicitarPagamento = solicitarPagamento;
        this.alterar = alterar;
        this.proporAlteracao = proporAlteracao;
    }

    @POST
    @APIResponse(responseCode = "201", description = "Processo criado",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = IdCriado.class)))
    public Response criar(@Valid @NotNull NovoProcesso dados) {
        Long id = criar.executar(dados.cotaId(), dados.tipo(), dados.valor(), dados.dadosFavorecidos());
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
    @Path("/{id}/aprovar-na-alcada")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual aprovarNaAlcada(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.APROVAR_NA_ALCADA, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/devolver-para-analise")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual devolverParaAnalise(Long id, @RequestBody(required = true) @Valid @NotNull Motivo corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.DEVOLVER_PARA_ANALISE, corpo.motivo()));
    }

    /** Só grava Pagamento solicitado: a rotina agendada é que fala com a tesouraria (R7, ADR 002). */
    @POST
    @Path("/{id}/solicitar-pagamento")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual solicitarPagamento(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(solicitarPagamento.executar(id, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/cancelar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual cancelar(Long id, @RequestBody(required = true) @Valid @NotNull Motivo corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.CANCELAR, corpo.motivo()));
    }

    @POST
    @Path("/{id}/solicitar-documento")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual solicitarDocumento(Long id, @RequestBody(required = true) @Valid @NotNull Motivo corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.SOLICITAR_DOCUMENTO, corpo.motivo()));
    }

    @POST
    @Path("/{id}/receber-documento")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual receberDocumento(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.RECEBER_DOCUMENTO, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/reprovar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual reprovar(Long id, @RequestBody(required = true) @Valid @NotNull Motivo corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.REPROVAR, corpo.motivo()));
    }

    @POST
    @Path("/{id}/alterar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual alterar(Long id, @Valid @NotNull Alteracao dados) {
        return EstadoAtual.de(alterar.executar(id, dados.valor(), NovoFavorecido.dados(dados.favorecidos())));
    }

    @POST
    @Path("/{id}/propor-alteracao")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual proporAlteracao(Long id, @Valid @NotNull PropostaAlteracao dados) {
        return EstadoAtual.de(proporAlteracao.executar(id, dados.valor(), NovoFavorecido.dados(dados.favorecidos()),
                dados.motivo()));
    }

    @POST
    @Path("/{id}/concordar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual concordar(Long id, @RequestBody(required = false) @Valid Observacao corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.CONCORDAR, Observacao.texto(corpo)));
    }

    @POST
    @Path("/{id}/discordar")
    @Consumes(MediaType.WILDCARD)
    public EstadoAtual discordar(Long id, @RequestBody(required = true) @Valid @NotNull Motivo corpo) {
        return EstadoAtual.de(executarAcao.executar(id, AcaoProcesso.DISCORDAR, corpo.motivo()));
    }
}
