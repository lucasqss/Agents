package br.com.claudio.consorcio.entrypoint.rest;

/**
 * Nomes dos papéis como vêm no claim {@code groups} do token (ADR 003), para as anotações {@code @RolesAllowed} dos
 * resources. É a primeira barreira, conferida antes de olhar o corpo e o processo (403 antes de 400, 404 e 409); o
 * papel certo por situação (reprovar, devolver para análise) é conferido depois, pela tabela do fluxo, no domínio.
 */
public final class PapeisDaApi {

    public static final String CLIENTE = "cliente";
    public static final String BACKOFFICE = "backoffice";
    public static final String APROVADOR = "aprovador";
    public static final String TESOURARIA = "tesouraria";

    private PapeisDaApi() {
    }
}
