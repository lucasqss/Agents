package br.com.claudio.consorcio.query.entrypoint.web;

import br.com.claudio.consorcio.query.application.DetalharCotaUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal web")
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
