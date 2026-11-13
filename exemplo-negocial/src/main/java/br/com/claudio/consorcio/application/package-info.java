/**
 * Casos de uso (Service Layer): application.&lt;agregado&gt;, uma classe por operação, com um único método público.
 * <p>Escrita: @Transactional, busca o agregado, chama o domínio e salva; pode chamar *Gateway. Consultas para a escrita usam as interfaces de repositório do domínio.
 * <p>Leitura: normaliza filtros e paginação e chama o *QueryRepository direto, sem domínio.
 * <p>Proibido: regra de negócio, JPA, *Entity, formatação e conhecer o canal.
 */
package br.com.claudio.consorcio.application;
