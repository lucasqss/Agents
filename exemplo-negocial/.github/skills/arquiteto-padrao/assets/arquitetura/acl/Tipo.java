package PACOTE_RAIZ.arquitetura;

import java.util.List;

/**
 * Tipo deste componente (ADR 001): traduz entre um sistema externo e o nosso vocabulário, sem regra nem dado de negócio.
 * Este arquivo vem da pasta do tipo ao instalar as regras. Trocar o tipo é decisão do dono da arquitetura.
 */
final class Tipo {

    static final String NOME = "ACL";

    /** As únicas pastas que podem existir debaixo do pacote raiz. */
    static final List<String> CAMADAS = List.of("entrypoint", "application", "translator", "gateway");

    private Tipo() {
    }
}
