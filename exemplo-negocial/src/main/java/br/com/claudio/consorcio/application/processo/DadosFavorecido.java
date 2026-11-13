package br.com.claudio.consorcio.application.processo;

import java.math.BigDecimal;

/** Um favorecido do pedido de abertura: quem recebe e quanto (R15, R29). */
public record DadosFavorecido(String nome, String documento, String papel, BigDecimal valor) {
}
