package br.com.claudio.consorcio.canal.taa.entrypoint;

import br.com.claudio.consorcio.query.application.usecase.DetalharCotaUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal TAA")
@Path("/taa/v1/cotas")
public class CotaTaaResource {

    private final DetalharCotaUseCase detalhar;

    public CotaTaaResource(DetalharCotaUseCase detalhar) {
        this.detalhar = detalhar;
    }

    @GET
    @Path("/{id}")
    public CotaTaaResponse detalhar(Long id) {
        return detalhar.executar(id).map(CotaTaaAssembler::montar)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }
}
