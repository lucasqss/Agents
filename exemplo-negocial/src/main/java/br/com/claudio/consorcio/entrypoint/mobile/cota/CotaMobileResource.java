package br.com.claudio.consorcio.entrypoint.mobile.cota;

import br.com.claudio.consorcio.application.cota.DetalharCotaUseCase;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal mobile")
@Path("/mobile/v1/cotas")
public class CotaMobileResource {

    private final DetalharCotaUseCase detalharCota;

    public CotaMobileResource(DetalharCotaUseCase detalharCota) {
        this.detalharCota = detalharCota;
    }

    @GET
    @Path("/{id}")
    public CotaMobileResponse cota(Long id) {
        return detalharCota.executar(id).map(CotaMobileAssembler::cota)
                .orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }
}
