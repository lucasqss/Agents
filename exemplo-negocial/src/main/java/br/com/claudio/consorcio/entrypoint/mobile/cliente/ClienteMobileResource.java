package br.com.claudio.consorcio.entrypoint.mobile.cliente;

import br.com.claudio.consorcio.application.cliente.ListarClientesUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal mobile")
@Path("/mobile/v1/clientes")
public class ClienteMobileResource {

    private final ListarClientesUseCase listarClientes;

    public ClienteMobileResource(ListarClientesUseCase listarClientes) {
        this.listarClientes = listarClientes;
    }

    @GET
    public ClientesMobileResponse clientes() {
        return ClienteMobileAssembler.clientes(listarClientes.executar());
    }
}
