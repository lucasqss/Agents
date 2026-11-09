package br.com.claudio.consorcio.persistence.processo;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.List;
import java.util.Optional;

@Repository
interface ProcessoPagamentoDataRepository {

    @Find
    Optional<ProcessoPagamentoEntity> buscarPorId(Long id);

    @Insert
    void inserir(ProcessoPagamentoEntity processo);

    @Update
    void atualizar(ProcessoPagamentoEntity processo);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.cotaId = :cotaId")
    long contarPorCota(Long cotaId);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.cotaId = :cotaId and p.estado not in :estadosFinais")
    long contarAtivosPorCota(Long cotaId, List<String> estadosFinais);
}
