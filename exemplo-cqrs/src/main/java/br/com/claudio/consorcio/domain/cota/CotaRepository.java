package br.com.claudio.consorcio.domain.cota;

import java.util.Optional;

public interface CotaRepository {

    Cota salvar(Cota cota);

    Optional<Cota> buscarPorId(Long id);

    boolean existeNumero(String numero, Long ignorarId);

    boolean existePorCliente(Long clienteId);

    void excluir(Long id);
}
