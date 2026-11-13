package br.com.claudio.consorcio.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class DinheiroTest {

    @Test
    void valoresIguaisSaoIguaisSejaQualForAEscalaInformada() {
        assertEquals(Dinheiro.de("10"), Dinheiro.de("10.00"));
        assertEquals(Dinheiro.de("10.5"), Dinheiro.de(new BigDecimal("10.500")));
        assertEquals(new BigDecimal("10.00"), Dinheiro.de("1E+1").valor());
    }

    @Test
    void recusaNuloEMaisDeDuasCasas() {
        assertThrows(DominioException.class, () -> Dinheiro.de((BigDecimal) null));
        assertThrows(DominioException.class, () -> Dinheiro.de("10.001"));
    }

    @Test
    void somaESubtrai() {
        assertEquals(Dinheiro.de("15.75"), Dinheiro.de("10.50").somar(Dinheiro.de("5.25")));
        assertEquals(Dinheiro.de("-0.01"), Dinheiro.de("0.10").subtrair(Dinheiro.de("0.11")));
    }

    @Test
    void multiplicaArredondandoOMeioCentavoParaOPar() {
        assertEquals(Dinheiro.de("0.12"), Dinheiro.de("0.25").multiplicar(new BigDecimal("0.5")));
        assertEquals(Dinheiro.de("0.18"), Dinheiro.de("0.35").multiplicar(new BigDecimal("0.5")));
        assertEquals(Dinheiro.de("33.33"), Dinheiro.de("100").multiplicar(new BigDecimal("0.3333")));
    }

    @Test
    void rateioNaoPerdeCentavo() {
        List<Dinheiro> partes = Dinheiro.de("100").ratear(3);
        assertEquals(List.of(Dinheiro.de("33.34"), Dinheiro.de("33.33"), Dinheiro.de("33.33")), partes);
        assertEquals(Dinheiro.de("100"), partes.stream().reduce(Dinheiro.ZERO, Dinheiro::somar));
    }

    @Test
    void rateioDeValorNegativoTambemFechaASoma() {
        List<Dinheiro> partes = Dinheiro.de("-0.05").ratear(3);
        assertEquals(List.of(Dinheiro.de("-0.02"), Dinheiro.de("-0.02"), Dinheiro.de("-0.01")), partes);
        assertThrows(DominioException.class, () -> Dinheiro.de("1").ratear(0));
    }

    @Test
    void compara() {
        assertTrue(Dinheiro.de("0.01").positivo());
        assertFalse(Dinheiro.ZERO.positivo());
        assertTrue(Dinheiro.de("2").maiorQue(Dinheiro.de("1.99")));
        assertFalse(Dinheiro.de("2").maiorQue(Dinheiro.de("2.00")));
    }
}
