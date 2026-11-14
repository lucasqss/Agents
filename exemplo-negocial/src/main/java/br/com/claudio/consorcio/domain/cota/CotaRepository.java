package br.com.claudio.consorcio.domain.cota;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public interface CotaRepository {

    Cota salvar(Cota cota);

    Optional<Cota> buscarPorId(Long id);

    /** Busca a cota e a mantém bloqueada até o fim da transação, para decisões que não podem correr em paralelo. */
    Optional<Cota> buscarPorIdComBloqueio(Long id);

    boolean existeNumero(String numero, @Nullable Long ignorarId);

    boolean existePorCliente(Long clienteId);

    void excluir(Long id);
}
