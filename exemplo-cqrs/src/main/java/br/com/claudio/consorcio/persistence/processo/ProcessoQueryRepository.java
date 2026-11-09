package br.com.claudio.consorcio.persistence.processo;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

@Repository
public interface ProcessoQueryRepository {

    @Query("""
            select new br.com.claudio.consorcio.persistence.processo.ProcessoResumo(
                p.id, c.id, c.numero, p.tipo, p.estado, p.protocoloPagamento)
            from ProcessoPagamentoEntity p
            join CotaEntity c on c.id = p.cotaId
            where (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            order by p.id
            """)
    Page<ProcessoResumo> listar(String tipo, String estado, PageRequest pageRequest);
}
