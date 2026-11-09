package br.com.claudio.consorcio.persistence.cota;

import br.com.claudio.consorcio.domain.cota.Cota;
import br.com.claudio.consorcio.domain.cota.CotaRepository;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

@ApplicationScoped
public class CotaRepositoryAdapter implements CotaRepository {

    private final CotaDataRepository dados;

    public CotaRepositoryAdapter(CotaDataRepository dados) {
        this.dados = dados;
    }

    @Override
    public Cota salvar(Cota cota) {
        CotaEntity entity = CotaMapper.paraEntidade(cota);
        if (cota.id() == null) {
            dados.inserir(entity);
        } else {
            dados.atualizar(entity);
        }
        return CotaMapper.paraDominio(entity);
    }

    @Override
    public Optional<Cota> buscarPorId(Long id) {
        return dados.buscarPorId(id).map(CotaMapper::paraDominio);
    }

    @Override
    public boolean existeNumero(String numero, Long ignorarId) {
        return dados.contarPorNumero(numero, ignorarId) > 0;
    }

    @Override
    public boolean existePorCliente(Long clienteId) {
        return dados.contarPorCliente(clienteId) > 0;
    }

    @Override
    public void excluir(Long id) {
        dados.excluir(id);
    }
}
