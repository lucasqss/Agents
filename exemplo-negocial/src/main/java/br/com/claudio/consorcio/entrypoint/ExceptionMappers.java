package br.com.claudio.consorcio.entrypoint;

import br.com.claudio.consorcio.application.ConflitoException;
import br.com.claudio.consorcio.application.NaoEncontradoException;
import br.com.claudio.consorcio.domain.AlteracaoConcorrenteException;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.cota.CreditoInsuficienteException;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.gateway.SistemaExternoIndisponivelException;
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

/** Traduz as exceções de validação, do domínio, da aplicação e das integrações para status HTTP. */
public class ExceptionMappers {

    private static final String DADOS_INVALIDOS = "Dados inválidos";

    @ServerExceptionMapper
    public Response transicaoInvalida(TransicaoInvalidaException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response creditoInsuficiente(CreditoInsuficienteException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response dominio(DominioException e) {
        return erro(Response.Status.BAD_REQUEST, e);
    }

    @ServerExceptionMapper
    public Response conflito(ConflitoException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response alteracaoConcorrente(AlteracaoConcorrenteException e) {
        return erro(Response.Status.CONFLICT, e);
    }

    @ServerExceptionMapper
    public Response naoEncontrado(NaoEncontradoException e) {
        return erro(Response.Status.NOT_FOUND, e);
    }

    @ServerExceptionMapper
    public Response sistemaExternoIndisponivel(SistemaExternoIndisponivelException e) {
        return erro(Response.Status.SERVICE_UNAVAILABLE, e);
    }

    /** Entrada fora do formato (Bean Validation): 400 com a lista de campos. */
    @ServerExceptionMapper
    public Response validacao(ConstraintViolationException e) {
        List<ErroResponse.CampoInvalido> campos = e.getConstraintViolations().stream()
                .map(v -> new ErroResponse.CampoInvalido(campo(v), v.getMessage()))
                .sorted(Comparator.comparing(ErroResponse.CampoInvalido::campo))
                .toList();
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErroResponse(DADOS_INVALIDOS, campos))
                .build();
    }

    /** Campo com tipo ou valor que o JSON não aceita: 400, indicando o campo quando ele é conhecido. */
    @ServerExceptionMapper
    public Response jsonIncompativel(MismatchedInputException e) {
        return Response.status(Response.Status.BAD_REQUEST)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErroResponse(DADOS_INVALIDOS, camposDoJson(e)))
                .build();
    }

    /** Recusas do próprio HTTP (rota, id que não é número, tipo de conteúdo): mantém o status e o formato único. */
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

    /**
     * Caminho do campo a partir do parâmetro: "criar.dados.cpf" vira "cpf"; item de lista mantém o índice
     * ("favorecidos[1].documento"); corpo ausente vira "dados".
     */
    private static String campo(ConstraintViolation<?> violacao) {
        StringBuilder caminho = new StringBuilder();
        String ultimo = null;
        boolean aposParametro = false;
        boolean indiceEscrito = false;
        for (Path.Node no : violacao.getPropertyPath()) {
            ultimo = no.getName();
            if (no.getKind() == ElementKind.PARAMETER) {
                aposParametro = true;
                continue;
            }
            if (!aposParametro) {
                continue;
            }
            if (no.isInIterable() && no.getIndex() != null && !indiceEscrito) {
                caminho.append('[').append(no.getIndex()).append(']');
                indiceEscrito = true;
            }
            if (no.getKind() == ElementKind.CONTAINER_ELEMENT) {
                continue;
            }
            if (!caminho.isEmpty()) {
                caminho.append('.');
            }
            caminho.append(no.getName());
            indiceEscrito = false;
        }
        return caminho.isEmpty() ? ultimo : caminho.toString();
    }

    private static Response erro(Response.Status status, RuntimeException e) {
        return Response.status(status).entity(ErroResponse.de(e.getMessage())).build();
    }
}
