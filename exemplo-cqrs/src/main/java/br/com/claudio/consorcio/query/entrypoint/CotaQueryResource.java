package br.com.claudio.consorcio.query.entrypoint;

import br.com.claudio.consorcio.query.application.DetalharCotaUseCase;
import br.com.claudio.consorcio.query.persistence.CotaDetalhe;
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
