package br.com.claudio.consorcio.canal.web.apresentacao;

import br.com.claudio.consorcio.canal.comum.apresentacao.PadraoApresentacao;
import java.math.BigDecimal;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Documentação viva do padrão do canal web: entrada -> saída esperada.
 * Mudou um formato? Quebra aqui, antes de quebrar a tela gerada pelo low-code.
 */
class PadraoWebTest {

    private static final PadraoApresentacao PADRAO = PadraoWeb.INSTANCIA;

    @ParameterizedTest(name = "{0}({1}) -> [{2}]")
    @CsvSource(delimiter = '|', nullValues = "null", textBlock = """
            moeda      | 250000.00                           | R$ 250.000,00
            moeda      | 1234567.89                          | R$ 1.234.567,89
            moeda      | 950.5                               | R$ 950,50
            cpf        | 12345678909                         | 123.456.789-09
            cpf        | 123.456.789-09                      | 123.456.789-09
            nome       | Júlia Araújo Lima                   | Júlia Araújo Lima
            nome       | Maria Aparecida dos Santos Oliveira | Maria Aparecida dos Santos Oliveira
            estado     | AGUARDANDO_VISTORIA                 | Aguardando vistoria
            estado     | EM_ANALISE                          | Em análise
            estado     | null                                | Sem processo
            tipo       | IMOBILIARIO                         | Imobiliário
            tipo       | SERVICO                             | Serviço
            quantidade | 1                                   | 1 cota
            quantidade | 3                                   | 3 cotas
            """)
    void formata(String formato, String entrada, String esperado) {
        BiFunction<PadraoApresentacao, String, String> aplicar = switch (formato) {
            case "moeda" -> (p, e) -> p.moeda(new BigDecimal(e));
            case "cpf" -> PadraoApresentacao::cpf;
            case "nome" -> PadraoApresentacao::nome;
            case "estado" -> PadraoApresentacao::estadoProcesso;
            case "tipo" -> PadraoApresentacao::tipoProcesso;
            case "quantidade" -> (p, e) -> p.quantidadeCotas(Long.parseLong(e));
            default -> throw new IllegalArgumentException(formato);
        };
        Assertions.assertEquals(esperado, aplicar.apply(PADRAO, entrada));
    }

    @Test
    void nuncaUsaEspacoNaoSeparavel() {
        // O NumberFormat pt-BR devolve "R$\u00a0250,00"; alguns fronts low-code exibem ou comparam isso errado.
        Assertions.assertFalse(PADRAO.moeda(new BigDecimal("950.00")).contains("\u00a0"));
    }
}
