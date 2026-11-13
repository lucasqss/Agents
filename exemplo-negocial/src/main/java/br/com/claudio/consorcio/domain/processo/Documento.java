package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.DominioException;

/**
 * Padrão: Value Object [PoEAA]. CPF (11 dígitos) ou CNPJ (14 dígitos), só com dígitos.
 * Nesta entrega o dígito verificador não é conferido.
 */
public record Documento(String numero) {

    private static final int TAMANHO_CPF = 11;
    private static final int TAMANHO_CNPJ = 14;

    public Documento {
        if (numero == null || numero.isBlank()) {
            throw new DominioException("Documento do favorecido é obrigatório");
        }
        boolean soDigitos = numero.chars().allMatch(c -> c >= '0' && c <= '9');
        if (!soDigitos || (numero.length() != TAMANHO_CPF && numero.length() != TAMANHO_CNPJ)) {
            throw new DominioException("Documento do favorecido deve ter só dígitos: 11 (CPF) ou 14 (CNPJ)");
        }
    }

    public static Documento de(String numero) {
        return new Documento(numero);
    }
}
