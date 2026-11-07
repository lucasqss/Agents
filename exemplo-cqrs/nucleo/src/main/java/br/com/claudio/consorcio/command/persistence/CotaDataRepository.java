package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.shared.persistence.CotaEntity;
import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.Optional;

@Repository
interface CotaDataRepository {

    @Find
    Optional<CotaEntity> buscarPorId(Long id);

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
