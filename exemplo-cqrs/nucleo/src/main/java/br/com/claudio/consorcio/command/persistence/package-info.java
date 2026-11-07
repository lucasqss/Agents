/**
 * Implementação dos repositórios do domínio, agrupada por tipo: repository (Jakarta Data), mapper
 * (domínio &lt;-> *Entity, à mão) e adapter (implementa a interface do domínio).
 * <p>Ninguém fora deste pacote o referencia; o acesso é pela interface do domínio. Mapper e repositório Jakarta
 * Data só são usados pelo adapter (CMD-011, CMD-012).
 */
package br.com.claudio.consorcio.command.persistence;
