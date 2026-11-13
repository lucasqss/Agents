package br.com.claudio.consorcio.persistence.cota;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.Optional;
import org.hibernate.LockMode;
import org.hibernate.StatelessSession;

@Repository
interface CotaDataRepository {

    /** Sessão do repositório, usada apenas onde o Jakarta Data não expressa a operação. */
    StatelessSession sessao();

    @Find
    Optional<CotaEntity> buscarPorId(Long id);

    default Optional<CotaEntity> buscarPorIdComBloqueio(Long id) {
        return Optional.ofNullable(sessao().get(CotaEntity.class, id, LockMode.PESSIMISTIC_WRITE));
    }

    @Insert
    void inserir(CotaEntity cota);

    @Update
    void atualizar(CotaEntity cota);

    @Query("delete from CotaEntity c where c.id = :id")
    void excluir(Long id);

    @Query("select count(c) from CotaEntity c where c.numero = :numero and (:ignorarId is null or c.id <> :ignorarId)")
    long contarPorNumero(String numero, Long ignorarId);

    @Query("select count(c) from CotaEntity c where c.clienteId = :clienteId")
    long contarPorCliente(Long clienteId);
}
