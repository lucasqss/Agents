package br.com.claudio.consorcio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;

/**
 * Propriedades do {@link Dinheiro} verificadas com valores gerados (jqwik): o que precisa valer para qualquer valor, não
 * só para os exemplos de {@link DinheiroTest}. Quando uma falha, o jqwik mostra o menor valor que a quebra.
 */
class DinheiroPropriedadesTest {

    @Property
    void rateioSempreFechaASoma(@ForAll("dinheiro") Dinheiro valor, @ForAll @IntRange(min = 1, max = 500) int partes) {
        List<Dinheiro> rateio = valor.ratear(partes);

        assertEquals(partes, rateio.size());
        assertEquals(valor, rateio.stream().reduce(Dinheiro.ZERO, Dinheiro::somar));
    }

    @Property
    void partesDoRateioDiferemEmNoMaximoUmCentavo(@ForAll("dinheiro") Dinheiro valor,
            @ForAll @IntRange(min = 1, max = 500) int partes) {
        List<Dinheiro> rateio = valor.ratear(partes);

        Dinheiro maior = rateio.stream().max(Dinheiro::compareTo).orElseThrow();
        Dinheiro menor = rateio.stream().min(Dinheiro::compareTo).orElseThrow();
        assertTrue(maior.subtrair(menor).compareTo(Dinheiro.de("0.01")) <= 0,
                () -> "partes de " + valor + " em " + partes + ": " + menor + " a " + maior);
    }

    @Property
    void subtrairDesfazASoma(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b) {
        assertEquals(a, a.somar(b).subtrair(b));
        assertEquals(a.somar(b), b.somar(a));
    }

    @Property
    void resultadoTemSempreDuasCasas(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b,
            @ForAll("fator") BigDecimal fator) {
        assertEquals(2, a.somar(b).valor().scale());
        assertEquals(2, a.subtrair(b).valor().scale());
        assertEquals(2, a.multiplicar(fator).valor().scale());
    }

    @Property
    void igualdadeNaoDependeDaEscalaInformada(@ForAll("centavos") long centavos,
            @ForAll @IntRange(min = 0, max = 6) int zerosAMais) {
        BigDecimal emDuasCasas = BigDecimal.valueOf(centavos, 2);
        BigDecimal comZerosAMais = emDuasCasas.setScale(2 + zerosAMais);

        assertEquals(Dinheiro.de(emDuasCasas), Dinheiro.de(comZerosAMais));
        assertEquals(Dinheiro.de(emDuasCasas).hashCode(), Dinheiro.de(comZerosAMais).hashCode());
    }

    @Property
    void comparacaoConcordaComOValor(@ForAll("dinheiro") Dinheiro a, @ForAll("dinheiro") Dinheiro b) {
        assertEquals(a.valor().compareTo(b.valor()) > 0, a.maiorQue(b));
        assertEquals(a.valor().signum() > 0, a.positivo());
    }

    @Provide
    Arbitrary<Long> centavos() {
        return Arbitraries.longs().between(-1_000_000_000_00L, 1_000_000_000_00L);
    }

    @Provide
    Arbitrary<Dinheiro> dinheiro() {
        return centavos().map(c -> Dinheiro.de(BigDecimal.valueOf(c, 2)));
    }

    @Provide
    Arbitrary<BigDecimal> fator() {
        return Arbitraries.bigDecimals().between(new BigDecimal("-10"), new BigDecimal("10")).ofScale(6);
    }
}
