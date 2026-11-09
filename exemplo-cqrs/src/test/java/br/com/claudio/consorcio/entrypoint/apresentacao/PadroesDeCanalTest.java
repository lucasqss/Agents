package br.com.claudio.consorcio.entrypoint.apresentacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.processo.TipoProcesso;
import br.com.claudio.consorcio.domain.processo.estado.EstadoProcesso;
import br.com.claudio.consorcio.entrypoint.mobile.PadraoMobile;
import br.com.claudio.consorcio.entrypoint.taa.PadraoTaa;
import br.com.claudio.consorcio.entrypoint.web.PadraoWeb;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Documentação viva dos padrões de cada canal: mesma entrada, uma saída esperada por canal.
 * Mudou um formato? Quebra aqui, antes de quebrar a tela gerada pelo low-code.
 */
class PadroesDeCanalTest {

    @ParameterizedTest(name = "{0}({1}) -> web [{2}] | mobile [{3}] | taa [{4}]")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            moeda      | 250000.00           | R$ 250.000,00       | R$ 250 mil       | R$250.000,00
            moeda      | 1234567.89          | R$ 1.234.567,89     | R$ 1,2 mi        | R$1.234.567,89
            moeda      | 950.5               | R$ 950,50           | R$ 950,50        | R$950,50
            cpf        | 12345678909         | 123.456.789-09      | ***.456.789-**   | ***.***.789-09
            cpf        | 123.456.789-09      | 123.456.789-09      | ***.456.789-**   | ***.***.789-09
            nome       | Júlia Araújo Lima   | Júlia Araújo Lima   | Júlia Lima       | JULIA ARAUJO LIMA
            nome       | Maria Aparecida dos Santos Oliveira | Maria Aparecida dos Santos Oliveira | Maria Oliveira | MARIA APARECIDA DOS
            estado     | AGUARDANDO_VISTORIA | Aguardando vistoria | Aguard. vistoria | AGUARDANDO VISTORIA
            estado     | EM_ANALISE          | Em análise          | Em análise       | EM ANALISE
            estado     | null                | Sem processo        | Sem processo     | SEM PROCESSO
            tipo       | IMOBILIARIO         | Imobiliário         | Imóvel           | IMOBILIARIO
            tipo       | SERVICO             | Serviço             | Serviço          | SERVICO
            quantidade | 1                   | 1 cota              | 1 cota           | 1 COTA
            quantidade | 3                   | 3 cotas             | 3 cotas          | 3 COTAS
            """)
    void cadaCanalTemSeuPadrao(String formato, String entrada, String web, String mobile, String taa) {
        BiFunction<PadraoApresentacao, String, String> aplicar = switch (formato) {
            case "moeda" -> (p, e) -> p.moeda(new BigDecimal(e));
            case "cpf" -> PadraoApresentacao::cpf;
            case "nome" -> PadraoApresentacao::nome;
            case "estado" -> PadraoApresentacao::estadoProcesso;
            case "tipo" -> PadraoApresentacao::tipoProcesso;
            case "quantidade" -> (p, e) -> p.quantidadeCotas(Long.parseLong(e));
            default -> throw new IllegalArgumentException(formato);
        };
        assertEquals(web, aplicar.apply(PadraoWeb.INSTANCIA, entrada), "web");
        assertEquals(mobile, aplicar.apply(PadraoMobile.INSTANCIA, entrada), "mobile");
        assertEquals(taa, aplicar.apply(PadraoTaa.INSTANCIA, entrada), "taa");
    }

    @Test
    void nuncaUsaEspacoNaoSeparavel() {
        // O NumberFormat pt-BR devolve "R$ 250,00"; alguns fronts low-code exibem ou comparam isso errado.
        for (PadraoApresentacao padrao : new PadraoApresentacao[] {PadraoWeb.INSTANCIA, PadraoMobile.INSTANCIA,
                PadraoTaa.INSTANCIA}) {
            assertFalse(padrao.moeda(new BigDecimal("950.00")).contains(" "), padrao.getClass().getSimpleName());
        }
    }

    @Test
    void todoEstadoETipoDoDominioTemRotulo() throws ReflectiveOperationException {
        // Um estado novo no domínio sem rótulo apareceria cru ("EM_REVISAO") na tela dos canais.
        for (Class<?> estado : EstadoProcesso.class.getPermittedSubclasses()) {
            String codigo = (String) estado.getField("NOME").get(null);
            assertTrue(Rotulos.temEstado(codigo), "estado sem rótulo: " + codigo);
        }
        for (TipoProcesso tipo : TipoProcesso.values()) {
            assertTrue(Rotulos.temTipo(tipo.name()), "tipo sem rótulo: " + tipo);
        }
    }
}
