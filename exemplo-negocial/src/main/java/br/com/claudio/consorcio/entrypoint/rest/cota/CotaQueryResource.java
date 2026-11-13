package br.com.claudio.consorcio.entrypoint.rest.cota;

import br.com.claudio.consorcio.application.cota.DetalharCotaUseCase;
import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;

@Path("/cotas")
public class CotaQueryResource {

    private final DetalharCotaUseCase detalhar;

    public CotaQueryResource(DetalharCotaUseCase detalhar) {
        this.detalhar = detalhar;
    }

    @GET
    @Path("/{id}")
    public CotaDetalhe detalhar(Long id) {
        return detalhar.executar(id).orElseThrow(() -> new NotFoundException("Cota " + id + " não encontrada"));
    }
}
