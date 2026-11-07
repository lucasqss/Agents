package br.com.claudio.consorcio.command.entrypoint;

import br.com.claudio.consorcio.command.application.ConflitoException;
import br.com.claudio.consorcio.command.application.NaoEncontradoException;
import br.com.claudio.consorcio.command.domain.DominioException;
import br.com.claudio.consorcio.command.domain.TransicaoInvalidaException;
import br.com.claudio.consorcio.shared.gateway.SistemaExternoIndisponivelException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import jakarta.ws.rs.core.Response;
import java.util.Comparator;
import java.util.List;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Traduz as exceções de validação, do domínio, da aplicação e das integrações para status HTTP. */
public class ExceptionMappers {

    @ServerExceptionMapper
    public Response transicaoInvalida(TransicaoInvalidaException e) {
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
                .entity(new ErroResponse("Dados inválidos", campos))
                .build();
    }

    /** Último nó do caminho da violação: "criar.dados.cpf" vira "cpf"; corpo ausente vira "dados". */
    private static String campo(ConstraintViolation<?> violacao) {
        String nome = null;
        for (Path.Node no : violacao.getPropertyPath()) {
            nome = no.getName();
        }
        return nome;
    }

    private static Response erro(Response.Status status, RuntimeException e) {
        return Response.status(status).entity(ErroResponse.de(e.getMessage())).build();
    }
}
