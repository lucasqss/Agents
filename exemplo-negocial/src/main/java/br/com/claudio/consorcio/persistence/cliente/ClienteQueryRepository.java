package br.com.claudio.consorcio.persistence.cliente;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

/** Repositório Jakarta Data: o Hibernate Processor gera a implementação e valida o HQL na compilação. */
@Repository
public interface ClienteQueryRepository {

    @Query("""
            select new br.com.claudio.consorcio.persistence.cliente.ClienteResumo(cl.id, cl.nome, cl.cpf, count(c.id))
            from ClienteEntity cl
            left join CotaEntity c on c.clienteId = cl.id
            group by cl.id, cl.nome, cl.cpf
            order by cl.nome
            """)
    List<ClienteResumo> listarComQuantidadeDeCotas();
}
