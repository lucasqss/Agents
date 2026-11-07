/**
 * REST de escrita: recebe HTTP (XRequest com Bean Validation para o formato), chama o use case e traduz
 * exceções para HTTP (ExceptionMappers).
 * <p>Proibido: regra de negócio e acesso a persistence.
 */
package br.com.claudio.consorcio.command.entrypoint;
