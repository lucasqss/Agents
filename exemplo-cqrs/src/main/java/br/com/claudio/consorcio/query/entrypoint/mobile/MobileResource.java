package br.com.claudio.consorcio.query.entrypoint.mobile;

import br.com.claudio.consorcio.query.application.DetalharCotaUseCase;
import br.com.claudio.consorcio.query.application.ListarClientesUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal mobile")
@Path("/mobile/v1")
public class MobileResource {

    private final DetalharCotaUseCase detalharCota;
    private final ListarClientesUseCase listarClientes;

    public MobileResource(DetalharCotaUseCase detalharCota, ListarClientesUseCase listarClientes) {
        this.detalharCota = detalharCota;
        this.listarClientes = listarClientes;
    }

    @GET
    @Path("/cotas/{id}")
    public CotaMobileResponse cota(Long id) {
        return detalharCota.executar(id).map(MobileAssembler::cota)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }

    @GET
    @Path("/clientes")
    public ClientesMobileResponse clientes() {
        return MobileAssembler.clientes(listarClientes.executar());
    }
}
