package br.com.claudio.consorcio.entrypoint.mobile.cota;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** O Assembler é função pura: testa sem Quarkus nem banco. */
class CotaMobileAssemblerTest {

    @Test
    void cotaSemProcesso() {
        CotaMobileResponse resposta = CotaMobileAssembler.cota(new CotaDetalhe(
                7L, "COTA-7", new BigDecimal("80000.00"), 1L, "Bruno", null, null, null));

        assertEquals("R$ 80 mil", resposta.valorCreditoFormatado());
        assertEquals(null, resposta.estado());
        assertEquals("Sem processo", resposta.situacao());
    }
}
