package br.com.claudio.pagamento.entrypoint;

import br.com.claudio.pagamento.application.NaoEncontradoException;
import br.com.claudio.pagamento.domain.AlteracaoConcorrenteException;
import br.com.claudio.pagamento.domain.DominioException;
import br.com.claudio.pagamento.domain.processo.fluxo.TransicaoInvalidaException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;
import org.jspecify.annotations.Nullable;

/** Traduz as exceções de validação, do domínio e da aplicação para status HTTP, sempre no formato de {@link ErroResponse}. */
public class ExceptionMappers {

    private static final String DADOS_INVALIDOS = "Dados inválidos";

    /** R2, R6: a ação não vale no estado atual do processo. */
    @ServerExceptionMapper
    public Response transicaoInvalida(TransicaoInvalidaException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response dominio(DominioException e) {
        return erro(Response.Status.BAD_REQUEST, e);
    }

    @ServerExceptionMapper
    public Response alteracaoConcorrente(AlteracaoConcorrenteException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response naoEncontrado(NaoEncontradoException e) {
        return erro(Response.Status.NOT_FOUND, e);
    }

    /** Entrada fora do formato (Bean Validation): 400 com a lista de campos. */
    @ServerExceptionMapper
    public Response validacao(ConstraintViolationException e) {
        List<ErroResponse.CampoInvalido> campos = e.getConstraintViolations().stream()
                .map(v -> new ErroResponse.CampoInvalido(campo(v), v.getMessage()))
                .sorted(Comparator.comparing(ErroResponse.CampoInvalido::campo,
                        Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErroResponse(DADOS_INVALIDOS, campos))
                .build();
    }

    /** Campo com tipo ou valor que o JSON não aceita (ex.: tipo de processo desconhecido): 400, indicando o campo. */
    @ServerExceptionMapper
    public Response jsonIncompativel(MismatchedInputException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(DADOS_INVALIDOS, camposDoJson(e)))
                .build();
    }

    /** Recusas do próprio HTTP (rota, ação ou id que não existe, tipo de conteúdo): mantém o status e o formato único. */
    @ServerExceptionMapper
    public Response recusaHttp(WebApplicationException e) {
        Response.StatusType status = e.getResponse().getStatusInfo();
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(ErroResponse.de(mensagemHttp(status)))
                .build();
    }

    private static List<ErroResponse.CampoInvalido> camposDoJson(MismatchedInputException e) {
        if (e.getPath().isEmpty() || e.getPath().getLast().getFieldName() == null) {
            return List.of();
        }
        return List.of(new ErroResponse.CampoInvalido(e.getPath().getLast().getFieldName(), "valor inválido"));
    }

    private static String mensagemHttp(Response.StatusType status) {
        return switch (status.getStatusCode()) {
            case 400 -> DADOS_INVALIDOS;
            case 404 -> "Recurso não encontrado";
            case 405 -> "Método não permitido";
            case 406 -> "Formato de resposta não suportado";
            case 415 -> "Tipo de conteúdo não suportado";
            default -> status.getReasonPhrase();
        };
    }

    /** Caminho do campo a partir do parâmetro: "abrir.dados.valor" vira "valor"; o cabeçalho vira o nome do parâmetro. */
    private static @Nullable String campo(ConstraintViolation<?> violacao) {
        StringBuilder caminho = new StringBuilder();
        String ultimo = null;
        boolean aposParametro = false;
        for (Path.Node no : violacao.getPropertyPath()) {
            ultimo = no.getName();
            if (no.getKind() == ElementKind.PARAMETER) {
                aposParametro = true;
                continue;
            }
            if (!aposParametro) {
                continue;
            }
            if (!caminho.isEmpty()) {
                caminho.append('.');
            }
            caminho.append(no.getName());
        }
        return caminho.isEmpty() ? ultimo : caminho.toString();
    }

    private static Response erro(Response.Status status, RuntimeException e) {
        return Response.status(status).entity(ErroResponse.de(e.getMessage())).build();
    }
}
