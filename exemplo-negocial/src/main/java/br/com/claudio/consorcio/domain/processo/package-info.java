/**
 * Processo de pagamento: um motor único ({@code ProcessoPagamento}) para todos os tipos.
 * <p>{@code fluxo}: o vocabulário do motor (estados, ações e a tabela de transições); não conhece tipo nenhum.
 * <p>{@code tipo}: tudo o que é específico de um tipo de processo. É aqui que um tipo novo entra.
 * <p>Dependências: fluxo &lt;- tipo &lt;- processo. O motor nunca faz {@code if (tipo == ...)}.
 */
package br.com.claudio.consorcio.domain.processo;
