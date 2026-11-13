package br.com.claudio.consorcio.domain.cota;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.cliente.Cliente;
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
        assertThrows(DominioException.class, () -> Cota.nova("001", Dinheiro.ZERO, 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", Dinheiro.de("-1"), 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", null, 1L));
        assertEquals(Dinheiro.de("1000"), Cota.nova("001", Dinheiro.de("1000"), 1L).valorCredito());
    }

    @Test
    void cotaExigeNumeroECliente() {
        assertThrows(DominioException.class, () -> Cota.nova(" ", Dinheiro.de("10"), 1L));
        assertThrows(DominioException.class, () -> Cota.nova("001", Dinheiro.de("10"), null));
    }
}
