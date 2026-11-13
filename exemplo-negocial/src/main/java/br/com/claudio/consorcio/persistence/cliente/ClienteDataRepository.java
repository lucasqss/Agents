package br.com.claudio.consorcio.persistence.cliente;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.Optional;

/** Jakarta Data: o Hibernate Processor gera a implementação e valida o HQL na compilação. */
@Repository
interface ClienteDataRepository {

    @Find
    Optional<ClienteEntity> buscarPorId(Long id);

    @Insert
    void inserir(ClienteEntity cliente);

    @Update
    void atualizar(ClienteEntity cliente);

    @Query("delete from ClienteEntity c where c.id = :id")
    void excluir(Long id);

    @Query("select count(c) from ClienteEntity c where c.cpf = :cpf and (:ignorarId is null or c.id <> :ignorarId)")
    long contarPorCpf(String cpf, Long ignorarId);
}
