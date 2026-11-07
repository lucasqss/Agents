/**
 * Estados do processo de pagamento (padrão State): sealed interface + records no MESMO pacote (exigência
 * do sealed); transição inválida lança TransicaoInvalidaException.
 */
package br.com.claudio.consorcio.command.domain.entity.pagamento.state;
