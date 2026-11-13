package br.com.claudio.consorcio.domain.cota;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import org.junit.jupiter.api.Test;

/** Regra R13: o crédito disponível é o crédito da cota menos o total dos processos pagos. */
class CotaCreditoTest {

    private static final Dinheiro CREDITO = Dinheiro.de("250000.00");
    private static final Dinheiro PAGO = Dinheiro.de("100000.00");

    private final Cota cota = Cota.reconstituir(1L, "COTA-1", CREDITO, 1L);

    @Test
    void semPagosODisponivelEOCreditoInteiro() {
        assertEquals(CREDITO, cota.creditoDisponivel(Dinheiro.ZERO));
    }

    @Test
    void disponivelEOCreditoMenosOsPagos() {
        assertEquals(Dinheiro.de("150000.00"), cota.creditoDisponivel(PAGO));
    }

    @Test
    void valorExatamenteIgualAoDisponivelEAceito() {
        assertDoesNotThrow(() -> cota.exigirCreditoPara(Dinheiro.de("150000.00"), PAGO));
    }

    @Test
    void umCentavoAcimaDoDisponivelEhRecusado() {
        CreditoInsuficienteException e = assertThrows(CreditoInsuficienteException.class,
                () -> cota.exigirCreditoPara(Dinheiro.de("150000.01"), PAGO));

        assertTrue(e.getMessage().contains("150000.00"), "a mensagem informa quanto há disponível: " + e.getMessage());
    }

    @Test
    void valorMenorQueODisponivelEAceito() {
        assertDoesNotThrow(() -> cota.exigirCreditoPara(Dinheiro.de("0.01"), PAGO));
    }

    @Test
    void creditoEsgotadoRecusaQualquerValor() {
        assertThrows(CreditoInsuficienteException.class, () -> cota.exigirCreditoPara(Dinheiro.de("0.01"), CREDITO));
    }
}
