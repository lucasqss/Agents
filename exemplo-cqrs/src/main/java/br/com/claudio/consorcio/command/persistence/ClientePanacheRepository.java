package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.shared.persistence.ClienteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
class ClientePanacheRepository implements PanacheRepository<ClienteEntity> {
}
