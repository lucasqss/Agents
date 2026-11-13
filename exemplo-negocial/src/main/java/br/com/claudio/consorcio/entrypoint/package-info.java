/**
 * Forma de acionamento: entrypoint.&lt;via&gt;.&lt;agregado&gt;, em que a via é rest, um canal (web, mobile, taa), mensageria ou batch.
 * <p>Na raiz ficam só ExceptionMappers, ErroResponse e IdCriado.
 * <p>Proibido: regra de negócio e acesso a *Entity, DataRepository, Mapper, *Gateway.
 */
package br.com.claudio.consorcio.entrypoint;
