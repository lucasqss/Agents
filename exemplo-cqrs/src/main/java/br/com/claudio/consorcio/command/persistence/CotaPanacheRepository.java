package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.shared.persistence.CotaEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class CotaPanacheRepository implements PanacheRepository<CotaEntity> {
}
