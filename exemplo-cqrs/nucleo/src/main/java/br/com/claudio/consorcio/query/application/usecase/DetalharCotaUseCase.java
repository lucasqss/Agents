package br.com.claudio.consorcio.query.application.usecase;

import br.com.claudio.consorcio.query.persistence.projection.CotaDetalhe;
import br.com.claudio.consorcio.query.persistence.repository.CotaQueryRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class DetalharCotaUseCase {

    private final CotaQueryRepository repository;

    public DetalharCotaUseCase(CotaQueryRepository repository) {
        this.repository = repository;
    }

    public Optional<CotaDetalhe> executar(Long cotaId) {
        return repository.detalhar(cotaId);
    }
}
