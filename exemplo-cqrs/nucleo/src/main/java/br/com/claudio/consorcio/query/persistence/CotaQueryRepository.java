package br.com.claudio.consorcio.query.persistence;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.Optional;

@Repository
public interface CotaQueryRepository {

    /** O processo atual é o de maior id da cota. */
    @Query("""
            select new br.com.claudio.consorcio.query.persistence.CotaDetalhe(
                c.id, c.numero, c.valorCredito, cl.id, cl.nome, p.id, p.tipo, p.estado)
            from CotaEntity c
            join ClienteEntity cl on cl.id = c.clienteId
            left join ProcessoPagamentoEntity p on p.cotaId = c.id
                and p.id = (select max(p2.id) from ProcessoPagamentoEntity p2 where p2.cotaId = c.id)
            where c.id = :id
            """)
    Optional<CotaDetalhe> detalhar(Long id);
}
