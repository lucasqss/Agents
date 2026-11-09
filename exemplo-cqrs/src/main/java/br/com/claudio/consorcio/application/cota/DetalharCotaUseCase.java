package br.com.claudio.consorcio.application.cota;

import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;
import br.com.claudio.consorcio.persistence.cota.CotaQueryRepository;
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
