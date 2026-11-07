package br.com.claudio.consorcio.command.domain.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.consorcio.command.domain.exception.DominioException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ClienteCotaTest {

    @Test
    void criaClienteValido() {
        Cliente cliente = Cliente.novo(" Maria ", "123.456.789-00");
        assertEquals("Maria", cliente.nome());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "  ")
    void clienteExigeNomeECpf(String vazio) {
        assertThrows(DominioException.class, () -> Cliente.novo(vazio, "123"));
        assertThrows(DominioException.class, () -> Cliente.novo("Maria", vazio));
    }

    @Test
    void cotaExigeCreditoPositivo() {
        assertThrows(DominioException.class, () -> Cota.nova("001", BigDecimal.ZERO, 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", new BigDecimal("-1"), 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", null, 1L));
        assertEquals(new BigDecimal("1000"), Cota.nova("001", new BigDecimal("1000"), 1L).valorCredito());
    }

    @Test
    void cotaExigeNumeroECliente() {
        assertThrows(DominioException.class, () -> Cota.nova(" ", BigDecimal.TEN, 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", BigDecimal.TEN, null));
    }
}
