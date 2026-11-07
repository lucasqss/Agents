package br.com.claudio.consorcio.canal.taa;

import br.com.claudio.consorcio.command.application.AvancarProcessoUseCase;
import br.com.claudio.consorcio.command.application.CriarProcessoUseCase;
import br.com.claudio.consorcio.command.application.DadosCliente;
import br.com.claudio.consorcio.command.application.DadosCota;
import br.com.claudio.consorcio.command.application.GerenciarClienteUseCase;
import br.com.claudio.consorcio.command.application.GerenciarCotaUseCase;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.ApplicationScoped;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prepara dados pelos use cases do núcleo, como o backoffice (web) faria: este canal não tem o CRUD.
 * Só existe nos testes.
 */
@ApplicationScoped
public class MassaDeDados {

    private static final AtomicLong SEQUENCIA = new AtomicLong(System.nanoTime() % 1_000_000_000L);
    private static final Solicitante BACKOFFICE = new Solicitante("teste", "WEB");

    private final GerenciarClienteUseCase clientes;
    private final GerenciarCotaUseCase cotas;
    private final CriarProcessoUseCase processos;
    private final AvancarProcessoUseCase avancar;

    public MassaDeDados(GerenciarClienteUseCase clientes, GerenciarCotaUseCase cotas, CriarProcessoUseCase processos,
            AvancarProcessoUseCase avancar) {
        this.clientes = clientes;
        this.cotas = cotas;
        this.processos = processos;
        this.avancar = avancar;
    }

    public long cliente(String nome) {
        return clientes.criar(new DadosCliente(nome, "%011d".formatted(SEQUENCIA.incrementAndGet())));
    }

    public long cota(long clienteId) {
        return cotas.criar(new DadosCota("COTA-Taa-" + SEQUENCIA.incrementAndGet(), new BigDecimal("250000"), clienteId));
    }

    /** Processo imobiliário parado em AGUARDANDO_VISTORIA. */
    public long processoAguardandoVistoria(long cotaId) {
        long id = processos.criar(BACKOFFICE, cotaId, TipoProcesso.IMOBILIARIO);
        avancar.iniciarAnalise(id);
        avancar.concluirAnalise(id);
        return id;
    }
}
