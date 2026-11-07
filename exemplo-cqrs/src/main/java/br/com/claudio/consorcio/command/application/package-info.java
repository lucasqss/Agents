/**
 * Use cases de escrita (Service Layer): @Transactional, buscam o agregado, chamam o domínio e salvam. Podem chamar *Gateway.
 * <p>Consultas para a escrita usam as interfaces de repositório do domínio.
 * <p>Proibido: regra de negócio, Panache, JPA, *Entity, command.persistence, query e conhecer o canal.
 */
package br.com.claudio.consorcio.command.application;
