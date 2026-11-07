package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cota;
import br.com.claudio.consorcio.command.domain.CotaRepository;
import br.com.claudio.consorcio.shared.persistence.CotaEntity;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class CotaRepositoryAdapter implements CotaRepository {

    private final CotaPanacheRepository panache;

    public CotaRepositoryAdapter(CotaPanacheRepository panache) {
        this.panache = panache;
    }

    @Override
    public Cota salvar(Cota cota) {
        CotaEntity entity = cota.id() == null ? new CotaEntity() : panache.findById(cota.id());
        CotaMapper.copiarParaEntidade(cota, entity);
        panache.persist(entity);
        return CotaMapper.paraDominio(entity);
    }

    @Override
    public Optional<Cota> buscarPorId(Long id) {
        return panache.findByIdOptional(id).map(CotaMapper::paraDominio);
    }

    @Override
    public boolean existeNumero(String numero, Long ignorarId) {
        return ignorarId == null
                ? panache.count("numero", numero) > 0
                : panache.count("numero = ?1 and id <> ?2", numero, ignorarId) > 0;
    }

    @Override
    public boolean existePorCliente(Long clienteId) {
        return panache.count("clienteId", clienteId) > 0;
    }

    @Override
    public void excluir(Long id) {
        panache.deleteById(id);
    }
}
