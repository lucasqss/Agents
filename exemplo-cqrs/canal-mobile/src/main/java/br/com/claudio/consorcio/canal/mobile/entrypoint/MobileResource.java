package br.com.claudio.consorcio.canal.mobile.entrypoint;

import br.com.claudio.consorcio.canal.comum.erro.IdCriado;
import br.com.claudio.consorcio.canal.comum.seguranca.SolicitanteDaRequisicao;
import br.com.claudio.consorcio.command.application.CriarProcessoUseCase;
import br.com.claudio.consorcio.query.application.DetalharCotaUseCase;
import br.com.claudio.consorcio.query.application.ListarClientesUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Tag(name = "Canal mobile")
@Path("/mobile/v1")
public class MobileResource {

    private final DetalharCotaUseCase detalharCota;
    private final ListarClientesUseCase listarClientes;
    private final CriarProcessoUseCase criarProcesso;
    private final SolicitanteDaRequisicao solicitante;

    public MobileResource(DetalharCotaUseCase detalharCota, ListarClientesUseCase listarClientes,
            CriarProcessoUseCase criarProcesso, SolicitanteDaRequisicao solicitante) {
        this.detalharCota = detalharCota;
        this.listarClientes = listarClientes;
        this.criarProcesso = criarProcesso;
        this.solicitante = solicitante;
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

    /** Mesmo use case do web; o domínio aplica a regra do canal (o app só abre processo de serviço). */
    @POST
    @Path("/processos")
    public Response abrirProcesso(@Valid @NotNull NovoProcessoMobileRequest dados) {
        Long id = criarProcesso.criar(solicitante.atual(), dados.cotaId(), dados.tipo());
        return Response.created(URI.create("/mobile/v1/processos/" + id)).entity(new IdCriado(id)).build();
    }
}
