package br.com.claudio.consorcio.entrypoint.web.cota;

import br.com.claudio.consorcio.application.cota.DetalharCotaUseCase;
import io.quarkus.security.Authenticated;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal web")
@Authenticated
@Path("/web/v1/cotas")
public class CotaWebResource {

    private final DetalharCotaUseCase detalhar;

    public CotaWebResource(DetalharCotaUseCase detalhar) {
        this.detalhar = detalhar;
    }

    @GET
    @Path("/{id}")
    public CotaWebResponse detalhar(Long id) {
        return detalhar.executar(id).map(CotaWebAssembler::montar)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }
}
