/**
 * Processo de pagamento. O <b>estado</b> (State) decide se a ação vale; o <b>fluxo do tipo</b>
 * (Strategy, {@code FluxoProcesso}) decide para onde ir quando os tipos divergem.
 * <p>Referências: [GoF] Gamma, Helm, Johnson, Vlissides. <i>Design Patterns</i>, 1994.
 * [PoEAA] Fowler. <i>Patterns of Enterprise Application Architecture</i>, 2002.
 */
package br.com.claudio.consorcio.domain.processo;
