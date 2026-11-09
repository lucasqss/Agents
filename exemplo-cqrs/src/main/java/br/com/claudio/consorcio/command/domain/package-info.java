/**
 * Domain Model (PoEAA): regras de negócio em Java puro e interfaces de repositório.
 * <p>Proibido: JPA, Jakarta Data, CDI, JAX-RS, Quarkus, Hibernate, MicroProfile e qualquer pacote shared ou de outra camada.
 * <p>Motor do processo de pagamento: o <b>estado</b> (State) decide se a ação vale; o <b>fluxo do tipo</b>
 * (Strategy, {@code FluxoProcesso}) decide para onde ir quando os tipos divergem.
 * <p>Referências: [GoF] Gamma, Helm, Johnson, Vlissides. <i>Design Patterns</i>, 1994.
 * [PoEAA] Fowler. <i>Patterns of Enterprise Application Architecture</i>, 2002.
 */
package br.com.claudio.consorcio.command.domain;
