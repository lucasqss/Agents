/**
 * Use cases de escrita (Service Layer): @Transactional, buscam o agregado, chamam o domínio e salvam. Podem chamar *Gateway.
 * <p>Consultas para a escrita usam as interfaces de repositório do domínio.
 * <p>Proibido: regra de negócio, Jakarta Data, JPA, *Entity, command.persistence, query, formatar e decidir por canal (o use case recebe o Solicitante e a regra por canal é do domínio: CanalDeAtendimento).
 */
package br.com.claudio.consorcio.command.application;
