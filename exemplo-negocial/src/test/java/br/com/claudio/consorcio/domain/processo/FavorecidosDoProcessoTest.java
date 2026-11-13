package br.com.claudio.consorcio.domain.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Invariantes dos favorecidos (R15, R29) protegidas pela raiz do agregado. */
class FavorecidosDoProcessoTest {

    private static final Dinheiro CEM = Dinheiro.de("100.00");

    private static Favorecido favorecido(String documento, String parte) {
        return new Favorecido("Nome " + documento, Documento.de(documento), "vendedor", Dinheiro.de(parte));
    }

    private static ProcessoPagamento abrir(Dinheiro valor, Favorecido... favorecidos) {
        return ProcessoPagamento.novo(TipoProcesso.SERVICO, 1L, valor, List.of(favorecidos));
    }

    private static DominioException recusa(Dinheiro valor, Favorecido... favorecidos) {
        return assertThrows(DominioException.class, () -> abrir(valor, favorecidos));
    }

    @Test
    void umFavorecidoComOValorInteiro() {
        ProcessoPagamento processo = abrir(CEM, favorecido("12345678901", "100.00"));

        assertEquals(1, processo.favorecidos().size());
    }

    @Test
    void partesSomandoOValorMantemAOrdemInformada() {
        ProcessoPagamento processo = abrir(Dinheiro.de("10000.00"), favorecido("12345678000199", "500.00"),
                favorecido("12345678901", "9500.00"));

        assertEquals("12345678000199", processo.favorecidos().get(0).documento().numero());
        assertEquals("12345678901", processo.favorecidos().get(1).documento().numero());
    }

    @Test
    void rateioComCentavosFechaExatamente() {
        ProcessoPagamento processo = abrir(CEM, favorecido("11111111111", "33.34"), favorecido("22222222222", "33.33"),
                favorecido("33333333333", "33.33"));

        assertEquals(3, processo.favorecidos().size());
    }

    @Test
    void somaUmCentavoAMenosERecusadaEDizQuantoFalta() {
        DominioException e = recusa(CEM, favorecido("11111111111", "60.00"), favorecido("22222222222", "39.99"));

        assertTrue(e.getMessage().contains("faltam 0.01"), e.getMessage());
    }

    @Test
    void somaUmCentavoAMaisERecusadaEDizQuantoPassa() {
        DominioException e = recusa(CEM, favorecido("11111111111", "60.00"), favorecido("22222222222", "40.01"));

        assertTrue(e.getMessage().contains("passam 0.01"), e.getMessage());
    }

    @Test
    void rateioQueArredondaCadaParteNaoFecha() {
        recusa(CEM, favorecido("11111111111", "33.33"), favorecido("22222222222", "33.33"),
                favorecido("33333333333", "33.33"));
        recusa(CEM, favorecido("11111111111", "33.34"), favorecido("22222222222", "33.34"),
                favorecido("33333333333", "33.34"));
    }

    @Test
    void mesmoDocumentoDuasVezesERecusadoMesmoComNomeEPapelDiferentes() {
        recusa(CEM, favorecido("12345678901", "50.00"),
                new Favorecido("Outro Nome", Documento.de("12345678901"), "cartório", Dinheiro.de("50.00")));
    }

    @Test
    void mesmoNomeComDocumentosDiferentesSaoFavorecidosDiferentes() {
        Favorecido a = new Favorecido("Ana", Documento.de("11111111111"), "vendedor", Dinheiro.de("50.00"));
        Favorecido b = new Favorecido("Ana", Documento.de("22222222222"), "vendedor", Dinheiro.de("50.00"));

        assertEquals(2, abrir(CEM, a, b).favorecidos().size());
    }

    @Test
    void semFavorecidoERecusado() {
        assertThrows(DominioException.class,
                () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, 1L, CEM, List.of()));
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, 1L, CEM, null));
    }

    @Test
    void parteZeroOuNegativaERecusada() {
        assertThrows(DominioException.class, () -> favorecido("11111111111", "0.00"));
        assertThrows(DominioException.class, () -> favorecido("11111111111", "-1.00"));
    }

    @Test
    void parteDeUmCentavoEAceita() {
        abrir(Dinheiro.de("0.01"), favorecido("11111111111", "0.01"));
    }

    @Test
    void nomeEPapelSaoObrigatorios() {
        Dinheiro parte = Dinheiro.de("1.00");
        Documento doc = Documento.de("11111111111");
        assertThrows(DominioException.class, () -> new Favorecido(null, doc, "vendedor", parte));
        assertThrows(DominioException.class, () -> new Favorecido("  ", doc, "vendedor", parte));
        assertThrows(DominioException.class, () -> new Favorecido("Ana", null, "vendedor", parte));
        assertThrows(DominioException.class, () -> new Favorecido("Ana", doc, null, parte));
        assertThrows(DominioException.class, () -> new Favorecido("Ana", doc, " ", parte));
    }

    @Test
    void listaComElementoNuloERecusada() {
        List<Favorecido> comNulo = new java.util.ArrayList<>();
        comNulo.add(null);
        assertThrows(DominioException.class, () -> ProcessoPagamento.novo(TipoProcesso.SERVICO, 1L, CEM, comNulo));
    }

    @Test
    void listaDoProcessoNaoPodeSerAlteradaPorFora() {
        ProcessoPagamento processo = abrir(CEM, favorecido("12345678901", "100.00"));

        assertThrows(UnsupportedOperationException.class, () -> processo.favorecidos().clear());
    }

    @Test
    void documentoAceitaCpfECnpj() {
        assertEquals("12345678901", Documento.de("12345678901").numero());
        assertEquals("12345678000199", Documento.de("12345678000199").numero());
    }

    @ParameterizedTest
    @ValueSource(strings = { "", " ", "1", "1234567890", "123456789012", "1234567890123", "123456789012345",
            "123.456.789-01", "1234567890a", "12345678901 ", "-2345678901" })
    void documentoForaDoFormatoERecusado(String numero) {
        assertThrows(DominioException.class, () -> Documento.de(numero));
    }

    @Test
    void documentoNuloERecusado() {
        assertThrows(DominioException.class, () -> Documento.de(null));
    }
}
