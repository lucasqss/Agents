/**
 * Forma de acionamento: entrypoint.&lt;via&gt;.&lt;agregado&gt;, em que a via é rest, um canal (web, mobile, taa), mensageria ou batch.
 * <p>Na raiz ficam só ExceptionMappers, ErroResponse e IdCriado.
 * <p>Único lugar com segurança (TEC-012): {@code @RolesAllowed}, {@code @Authenticated} e o token JWT. O entrypoint monta o
 * {@code Autor} a partir do token e o entrega ao use case (ADR 003).
 * <p>Proibido: regra de negócio e acesso a *Entity, DataRepository, Mapper, *Gateway.
 */
package br.com.claudio.consorcio.entrypoint;
