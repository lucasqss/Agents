package br.com.claudio.pagamento.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DinheiroTest {

    @Test
    void escalaNaoDecideComparacao() {
        assertEquals(Dinheiro.de("30000"), Dinheiro.de("30000.00"));
        assertEquals(0, Dinheiro.de("10.5").compareTo(Dinheiro.de("10.50")));
    }

    @ParameterizedTest(name = "{0} > {1}: {2}")
    @CsvSource({"50000.01, 50000.00, true", "50000.00, 50000.00, false", "49999.99, 50000.00, false"})
    void maiorQue(String valor, String outro, boolean esperado) {
        assertEquals(esperado, Dinheiro.de(valor).maiorQue(Dinheiro.de(outro)));
    }

    @Test
    void positivoSoAcimaDeZero() {
        assertTrue(Dinheiro.de("0.01").positivo());
        assertFalse(Dinheiro.de("0").positivo());
        assertFalse(Dinheiro.de("-1").positivo());
    }

    @Test
    void recusaMaisDeDuasCasasENulo() {
        assertThrows(DominioException.class, () -> Dinheiro.de("1.001"));
        assertThrows(DominioException.class, () -> Dinheiro.de((BigDecimal) null));
    }
}
