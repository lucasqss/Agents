package br.com.claudio.consorcio.entrypoint.rest.processo;

import br.com.claudio.consorcio.domain.processo.Autor;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Único lugar que lê o token da pessoa: extrai dele o {@link Autor}, dado puro que os casos de uso e o domínio recebem.
 * Usuário é o claim {@code sub}; papéis, o {@code groups} (nomes desconhecidos são ignorados); o cadastro do cliente, o
 * {@code clienteId} (ausente ou que não é número: sem cadastro).
 */
final class AutorDoToken {

    static final String CLAIM_CLIENTE_ID = "clienteId";

    private AutorDoToken() {
    }

    static Autor de(JsonWebToken token) {
        return new Autor(token.getSubject(), papeis(token), clienteId(token));
    }

    private static Set<Papel> papeis(JsonWebToken token) {
        Set<Papel> papeis = EnumSet.noneOf(Papel.class);
        Set<String> grupos = token.getGroups();
        if (grupos != null) {
            for (String grupo : grupos) {
                for (Papel papel : Papel.values()) {
                    if (papel.name().toLowerCase(Locale.ROOT).equals(grupo)) {
                        papeis.add(papel);
                    }
                }
            }
        }
        return papeis;
    }

    private static Long clienteId(JsonWebToken token) {
        Object valor = token.getClaim(CLAIM_CLIENTE_ID);
        if (valor == null) {
            return null;
        }
        try {
            return Long.valueOf(valor.toString().strip());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
