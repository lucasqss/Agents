package br.com.claudio.consorcio.entrypoint.mobile.cliente;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.claudio.consorcio.persistence.cliente.ClienteResumo;
import java.util.List;
import org.junit.jupiter.api.Test;

/** O Assembler é função pura: testa sem Quarkus nem banco. */
class ClienteMobileAssemblerTest {

    @Test
    void somaAsCotasDosClientesParaORodape() {
        ClientesMobileResponse resposta = ClienteMobileAssembler.clientes(List.of(
                new ClienteResumo(1L, "Júlia Araújo Lima", "12345678909", 2L),
                new ClienteResumo(2L, "Bruno", "98765432100", 1L)));

        assertEquals(3, resposta.totalCotas());
        assertEquals("3 cotas", resposta.totalCotasDescricao());
        assertEquals("Júlia Lima", resposta.itens().getFirst().nome());
        assertEquals("***.456.789-**", resposta.itens().getFirst().cpf());
        assertEquals("1 cota", resposta.itens().getLast().quantidadeCotasDescricao());
    }
}
