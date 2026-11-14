package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import java.util.Set;

/**
 * Quem pede uma ação sobre o processo: o usuário, os papéis que ele tem e, quando é cliente, o cadastro de cliente
 * (R17, R18, R19). Dado puro: o entrypoint o monta a partir da identidade verificada e o domínio só o lê; nada aqui
 * sabe de token nem de HTTP. Uma pessoa pode ter mais de um papel.
 */
public record Autor(String usuario, Set<Papel> papeis, Long clienteId) {

    public Autor {
        if (usuario == null || usuario.isBlank()) {
            throw new DominioException("Autor da ação é obrigatório");
        }
        usuario = usuario.strip();
        papeis = papeis == null ? Set.of() : Set.copyOf(papeis);
    }

    public boolean temAlgumDosPapeis(Set<Papel> procurados) {
        return papeis.stream().anyMatch(procurados::contains);
    }
}
