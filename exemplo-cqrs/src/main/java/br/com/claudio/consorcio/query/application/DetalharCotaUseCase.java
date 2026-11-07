package br.com.claudio.consorcio.query.application;

import br.com.claudio.consorcio.query.persistence.CotaDetalhe;
import br.com.claudio.consorcio.query.persistence.CotaQueryRepository;
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
