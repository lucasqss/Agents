package br.com.claudio.consorcio.entrypoint.rest.cliente;

import br.com.claudio.consorcio.application.cliente.ListarClientesUseCase;
import br.com.claudio.consorcio.persistence.cliente.ClienteResumo;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

/** Consulta: qualquer papel autenticado (ADR 003). */
@Authenticated
@Path("/clientes")
public class ClienteQueryResource {

    private final ListarClientesUseCase listar;

    public ClienteQueryResource(ListarClientesUseCase listar) {
        this.listar = listar;
    }

    @GET
    public List<ClienteResumo> listar() {
        return listar.executar();
    }
}
