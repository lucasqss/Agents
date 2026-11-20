package br.com.claudio.pagamento.persistence.processo;

import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import java.util.List;
import java.util.Optional;

/** Leitura de tela: HQL validado na compilação e records projetados, sem passar pelo domínio. */
@Repository
public interface ProcessoQueryRepository {

    @Query("""
            select new br.com.claudio.pagamento.persistence.processo.ProcessoResumo(
                p.id, p.tipo, p.numeroCota, p.valor, p.estado)
            from ProcessoPagamentoEntity p
            where p.id = :id
            """)
    Optional<ProcessoResumo> detalhar(Long id);

    @Query("select count(p) from ProcessoPagamentoEntity p where p.id = :id")
    long contar(Long id);

    @Query("""
            select new br.com.claudio.pagamento.persistence.processo.ProcessoTransicaoResumo(
                t.estadoOrigem, t.estadoDestino, t.acao, t.ocorridoEm, t.autor, t.observacao)
            from ProcessoTransicaoEntity t
            where t.processoId = :processoId
            order by t.id
            """)
    List<ProcessoTransicaoResumo> historico(Long processoId);
}
