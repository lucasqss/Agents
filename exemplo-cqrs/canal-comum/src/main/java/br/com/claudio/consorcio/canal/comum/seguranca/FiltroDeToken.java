package br.com.claudio.consorcio.canal.comum.seguranca;

import br.com.claudio.consorcio.canal.comum.erro.ErroResponse;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.Optional;
import org.jboss.resteasy.reactive.server.ServerRequestFilter;

/**
 * Exige o token do canal nos caminhos do canal e entrega a identidade comum ao resto da requisição.
 * Igual para todos os canais; o que muda é o MapeadorDeToken de cada módulo. Como cada canal é um
 * deployable, o filtro de um canal nunca intercepta o outro.
 */
public class FiltroDeToken {

    private static final String BEARER = "Bearer ";

    private final MapeadorDeToken mapeador;
    private final SolicitanteDaRequisicao solicitante;

    public FiltroDeToken(MapeadorDeToken mapeador, SolicitanteDaRequisicao solicitante) {
        this.mapeador = mapeador;
        this.solicitante = solicitante;
    }

    @ServerRequestFilter(priority = Priorities.AUTHENTICATION)
    public Optional<Response> autenticar(ContainerRequestContext requisicao) {
        if (!requisicao.getUriInfo().getPath().startsWith(mapeador.caminhosProtegidos())) {
            return Optional.empty();
        }
        String cabecalho = requisicao.getHeaderString(HttpHeaders.AUTHORIZATION);
        Optional<Solicitante> identidade = cabecalho != null && cabecalho.startsWith(BEARER)
                ? mapeador.mapear(cabecalho.substring(BEARER.length()).strip())
                : Optional.empty();
        if (identidade.isEmpty()) {
            return Optional.of(Response.status(Response.Status.UNAUTHORIZED)
                    .entity(ErroResponse.de("Token do canal ausente ou inválido"))
                    .build());
        }
        solicitante.definir(identidade.get());
        return Optional.empty();
    }
}
