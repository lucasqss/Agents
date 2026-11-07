package br.com.claudio.consorcio.command.entrypoint;

import br.com.claudio.consorcio.command.application.ConflitoException;
import br.com.claudio.consorcio.command.application.NaoEncontradoException;
import br.com.claudio.consorcio.command.domain.DominioException;
import br.com.claudio.consorcio.command.domain.TransicaoInvalidaException;
import br.com.claudio.consorcio.shared.gateway.SistemaExternoIndisponivelException;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Traduz as exceções do domínio, da aplicação e das integrações para status HTTP. */
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

    private static Response erro(Response.Status status, RuntimeException e) {
        return Response.status(status).entity(new ErroResponse(e.getMessage())).build();
    }
}
