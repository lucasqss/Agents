package br.com.claudio.consorcio.persistence.processo;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.List;
import java.util.Optional;
import org.hibernate.LockMode;
import org.hibernate.StatelessSession;

@Repository
interface ProcessoPagamentoDataRepository {

    /** Sessão do repositório, usada apenas onde o Jakarta Data não expressa a operação. */
    StatelessSession sessao();

    @Find
    Optional<ProcessoPagamentoEntity> buscarPorId(Long id);

    default Optional<ProcessoPagamentoEntity> buscarPorIdComBloqueio(Long id) {
        return Optional.ofNullable(sessao().get(ProcessoPagamentoEntity.class, id, LockMode.PESSIMISTIC_WRITE));
    }

    @Insert
    void inserir(ProcessoPagamentoEntity processo);

    @Update
    void atualizar(ProcessoPagamentoEntity processo);

    @Insert
    void inserirTransicao(ProcessoTransicaoEntity transicao);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.cotaId = :cotaId")
    long contarPorCota(Long cotaId);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.cotaId = :cotaId and p.estado not in :estadosFinais")
    long contarAtivosPorCota(Long cotaId, List<String> estadosFinais);
}
