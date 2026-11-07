package br.com.claudio.consorcio.canal.mobile.entrypoint;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.claudio.consorcio.query.persistence.ClienteResumo;
import br.com.claudio.consorcio.query.persistence.CotaDetalhe;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O Assembler é função pura: testa sem Quarkus nem banco. */
class MobileAssemblerTest {

    @Test
    void somaAsCotasDosClientesParaORodape() {
        ClientesMobileResponse resposta = MobileAssembler.clientes(List.of(
                new ClienteResumo(1L, "Júlia Araújo Lima", "12345678909", 2L),
                new ClienteResumo(2L, "Bruno", "98765432100", 1L)));

        assertEquals(3, resposta.totalCotas());
        assertEquals("3 cotas", resposta.totalCotasDescricao());
        assertEquals("Júlia Lima", resposta.itens().getFirst().nome());
        assertEquals("***.456.789-**", resposta.itens().getFirst().cpf());
        assertEquals("1 cota", resposta.itens().getLast().quantidadeCotasDescricao());
    }

    @Test
    void cotaSemProcesso() {
        CotaMobileResponse resposta = MobileAssembler.cota(new CotaDetalhe(
                7L, "COTA-7", new BigDecimal("80000.00"), 1L, "Bruno", null, null, null));

        assertEquals("R$ 80 mil", resposta.valorCreditoFormatado());
        assertEquals(null, resposta.estado());
        assertEquals("Sem processo", resposta.situacao());
    }
}
