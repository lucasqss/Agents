package br.com.claudio.consorcio.query.entrypoint;

import br.com.claudio.consorcio.query.application.ListarClientesUseCase;
import br.com.claudio.consorcio.query.persistence.ClienteResumo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import java.util.List;

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
