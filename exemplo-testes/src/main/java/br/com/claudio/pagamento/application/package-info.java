/**
 * Casos de uso (Service Layer): application.&lt;agregado&gt;, uma classe por operação, com um único método público.
 * <p>Escrita: @Transactional, busca o agregado pela interface do domínio, chama o domínio e salva.
 * <p>Leitura: chama o *QueryRepository direto, sem domínio.
 * <p>Proibido: regra de negócio, JPA, *Entity, formatação e conhecer o canal.
 */
package br.com.claudio.pagamento.application;
