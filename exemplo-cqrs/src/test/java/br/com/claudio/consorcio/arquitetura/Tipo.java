package br.com.claudio.consorcio.arquitetura;

import java.util.List;

/**
 * Tipo deste componente (ADR 001): regras e dados de um bounded context, com um banco só dele.
 * Este arquivo vem da pasta do tipo ao instalar as regras. Trocar o tipo é decisão do dono da arquitetura.
 */
final class Tipo {

    static final String NOME = "Negocial";

    /** As únicas pastas que podem existir debaixo do pacote raiz. */
    static final List<String> CAMADAS = List.of("entrypoint", "application", "domain", "persistence", "gateway");

    private Tipo() {
    }
}
