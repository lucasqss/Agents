package br.com.claudio.pagamento.persistence.processo;

import jakarta.data.repository.Find;
import jakarta.data.repository.Insert;
import jakarta.data.repository.Repository;
import jakarta.data.repository.Update;
import java.util.Optional;

/** Jakarta Data: o Hibernate Processor gera a implementação na compilação. Só a escrita do agregado. */
@Repository
interface ProcessoPagamentoDataRepository {

    @Find
    Optional<ProcessoPagamentoEntity> buscarPorId(Long id);

    @Insert
    void inserir(ProcessoPagamentoEntity processo);

    @Update
    void atualizar(ProcessoPagamentoEntity processo);

    @Insert
    void inserirTransicao(ProcessoTransicaoEntity transicao);
}
