package br.com.claudio.consorcio.domain;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Padrão: Money [PoEAA], como Value Object. Valor monetário em reais, sempre com duas casas.
 * Toda conta com dinheiro passa por aqui, para que arredondamento e rateio tenham uma regra só.
 * Se o sistema passar a ter mais de uma moeda, ela entra como segundo componente deste record,
 * e as operações recusam misturar moedas.
 */
public record Dinheiro(BigDecimal valor) implements Comparable<Dinheiro> {

    private static final int CASAS = 2;

    public static final Dinheiro ZERO = new Dinheiro(BigDecimal.ZERO);

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

    public Dinheiro somar(Dinheiro outro) {
        return new Dinheiro(valor.add(outro.valor));
    }

    public Dinheiro subtrair(Dinheiro outro) {
        return new Dinheiro(valor.subtract(outro.valor));
    }

    /** Multiplica por um fator (ex.: taxa) e arredonda o centavo para o par mais próximo. */
    public Dinheiro multiplicar(BigDecimal fator) {
        return new Dinheiro(valor.multiply(fator).setScale(CASAS, RoundingMode.HALF_EVEN));
    }

    /**
     * Divide em partes que somam exatamente o valor original: os centavos que sobram da divisão
     * vão um a um para as primeiras partes. Dividir e arredondar cada parte perderia ou criaria centavos.
     */
    public List<Dinheiro> ratear(int partes) {
        if (partes <= 0) {
            throw new DominioException("Rateio precisa de ao menos uma parte");
        }
        BigInteger[] divisao = valor.unscaledValue().divideAndRemainder(BigInteger.valueOf(partes));
        int sobra = divisao[1].abs().intValue();
        BigInteger centavo = BigInteger.valueOf(divisao[1].signum());
        List<Dinheiro> resultado = new ArrayList<>(partes);
        for (int i = 0; i < partes; i++) {
            BigInteger centavos = i < sobra ? divisao[0].add(centavo) : divisao[0];
            resultado.add(new Dinheiro(new BigDecimal(centavos, CASAS)));
        }
        return List.copyOf(resultado);
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
