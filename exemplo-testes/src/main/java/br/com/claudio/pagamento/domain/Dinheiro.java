package br.com.claudio.pagamento.domain;

import java.math.BigDecimal;

/**
 * Padrão: Money [PoEAA], como Value Object. Valor monetário em reais, sempre com duas casas.
 * Toda comparação de dinheiro passa por aqui, para que a escala nunca decida um resultado (30000.0 == 30000.00).
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    private static final int CASAS = 2;

    public Dinheiro {
        if (valor == null) {
            throw new DominioException("Valor monetário é obrigatório");
        }
        if (valor.stripTrailingZeros().scale() > CASAS) {
            throw new DominioException("Valor monetário tem no máximo " + CASAS + " casas decimais");
        }
        valor = valor.setScale(CASAS);
    }

    public static Dinheiro de(BigDecimal valor) {
        return new Dinheiro(valor);
    }

    public static Dinheiro de(String valor) {
        return new Dinheiro(new BigDecimal(valor));
    }

    public boolean positivo() {
        return valor.signum() > 0;
    }

    public boolean maiorQue(Dinheiro outro) {
        return compareTo(outro) > 0;
    }

    @Override
    public int compareTo(Dinheiro outro) {
        return valor.compareTo(outro.valor);
    }
}
