package br.com.claudio.consorcio.persistence.processo;

import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;

@Repository
public interface ProcessoQueryRepository {

    @Query("""
            select new br.com.claudio.consorcio.persistence.processo.ProcessoResumo(
                p.id, c.id, c.numero, p.tipo, p.valor, p.estado, p.protocoloPagamento)
            from ProcessoPagamentoEntity p
            join CotaEntity c on c.id = p.cotaId
            where (:tipo is null or p.tipo = :tipo)
              and (:estado is null or p.estado = :estado)
            order by p.id
            """)
    Page<ProcessoResumo> listar(String tipo, String estado, PageRequest pageRequest);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.id = :id")
    long contar(Long id);

    @Query("""
            select new br.com.claudio.consorcio.persistence.processo.ProcessoTransicaoResumo(
                t.estadoOrigem, t.estadoDestino, t.acao, t.ocorridoEm, t.observacao)
            from ProcessoTransicaoEntity t
            where t.processoId = :processoId
            order by t.id
            """)
    List<ProcessoTransicaoResumo> historico(Long processoId);

    @Query("""
            select new br.com.claudio.consorcio.persistence.processo.ProcessoFavorecidoResumo(
                f.nome, f.documento, f.papel, f.valor)
            from ProcessoFavorecidoEntity f
            where f.processoId = :processoId
            order by f.ordem
            """)
    List<ProcessoFavorecidoResumo> favorecidos(Long processoId);
}
