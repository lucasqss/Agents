/**
 * Nosso banco: persistence.&lt;agregado&gt;. Entidades JPA (*Entity): só campos, mapeamento, getters e setters.
 * <p>Escrita: DataRepository (Jakarta Data, stateless: gravar é sempre explícito), Mapper à mão e RepositoryAdapter, que implementa a interface do domínio. Ninguém fora daqui os referencia.
 * <p>Leitura de tela: *QueryRepository com @Query em HQL (validado na compilação) e os records projetados.
 * <p>Proibido: SQL nativo e depender de application ou entrypoint.
 */
package br.com.claudio.consorcio.persistence;
