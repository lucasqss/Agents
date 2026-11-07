package br.com.claudio.consorcio.query.application.usecase;

import br.com.claudio.consorcio.query.application.dto.Pagina;
import br.com.claudio.consorcio.query.persistence.projection.ProcessoResumo;
import br.com.claudio.consorcio.query.persistence.repository.ProcessoQueryRepository;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ListarProcessosUseCase {

    private static final int TAMANHO_MAXIMO = 100;

    private final ProcessoQueryRepository repository;

    public ListarProcessosUseCase(ProcessoQueryRepository repository) {
        this.repository = repository;
    }

    /** Filtros vazios são ignorados; a página começa em 1 e o tamanho vai de 1 a 100. */
    public Pagina<ProcessoResumo> executar(String tipo, String estado, int pagina, int tamanho) {
        int paginaValida = Math.max(pagina, 1);
        int tamanhoValido = Math.clamp(tamanho, 1, TAMANHO_MAXIMO);
        Page<ProcessoResumo> page = repository.listar(vazioComoNulo(tipo), vazioComoNulo(estado),
                PageRequest.ofPage(paginaValida).size(tamanhoValido));
        return new Pagina<>(page.content(), paginaValida, tamanhoValido, page.totalElements());
    }

    private static String vazioComoNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip().toUpperCase();
    }
}
