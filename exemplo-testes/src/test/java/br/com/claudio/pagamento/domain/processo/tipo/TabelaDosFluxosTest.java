package br.com.claudio.pagamento.domain.processo.tipo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.TransicaoInvalidaException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Matriz exaustiva (R2, R3, R4, R6, R7): a tabela de cada tipo, escrita por extenso, é comparada com a tabela que o código
 * monta, em todas as combinações de estado e ação. Toda linha que existe no código e não aqui (ou o contrário) falha,
 * mostrando a diferença. Mudar o fluxo é, de propósito, mudar este arquivo também: quem revisa vê a regra mudando.
 *
 * <p>Formato: {@code ESTADO AÇÃO -> DESTINO}. Na linha condicional da alçada: {@code -> acima: X | até: Y}.
 */
class TabelaDosFluxosTest {

    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");
    private static final ContextoDaTransicao ACIMA = new ContextoDaTransicao(Dinheiro.de("50000.01"), ALCADA);
    private static final ContextoDaTransicao ATE = new ContextoDaTransicao(ALCADA, ALCADA);

    private static final String COMUM = """
            ABERTO INICIAR_ANALISE -> EM_ANALISE
            ABERTO CANCELAR -> CANCELADO
            EM_ANALISE SOLICITAR_DOCUMENTO -> PENDENTE_DOCUMENTO
            EM_ANALISE REPROVAR -> REPROVADO
            EM_ANALISE CANCELAR -> CANCELADO
            PENDENTE_DOCUMENTO RECEBER_DOCUMENTO -> EM_ANALISE
            PENDENTE_DOCUMENTO CANCELAR -> CANCELADO
            AGUARDANDO_ALCADA APROVAR_NA_ALCADA -> APROVADO
            AGUARDANDO_ALCADA DEVOLVER_PARA_ANALISE -> EM_ANALISE
            AGUARDANDO_ALCADA REPROVAR -> REPROVADO
            AGUARDANDO_ALCADA CANCELAR -> CANCELADO
            APROVADO DEVOLVER_PARA_ANALISE -> EM_ANALISE
            APROVADO PAGAR -> PAGO
            APROVADO CANCELAR -> CANCELADO
            """;

    @Test
    void servico() {
        assertTabela(TipoProcesso.SERVICO, COMUM + """
                EM_ANALISE CONCLUIR_ANALISE -> acima: AGUARDANDO_ALCADA | até: APROVADO
                """);
    }

    @Test
    void imobiliarioPassaPelaVistoria() {
        assertTabela(TipoProcesso.IMOBILIARIO, COMUM + """
                EM_ANALISE CONCLUIR_ANALISE -> AGUARDANDO_VISTORIA
                AGUARDANDO_VISTORIA CONCLUIR_VISTORIA -> acima: AGUARDANDO_ALCADA | até: APROVADO
                AGUARDANDO_VISTORIA DEVOLVER_PARA_ANALISE -> EM_ANALISE
                AGUARDANDO_VISTORIA CANCELAR -> CANCELADO
                """);
    }

    private static void assertTabela(TipoProcesso tipo, String esperada) {
        assertEquals(ordenar(esperada.lines().toList()), ordenar(tabelaDoCodigo(tipo)),
                "Tabela do fluxo " + tipo + " diferente da especificada");
    }

    private static List<String> tabelaDoCodigo(TipoProcesso tipo) {
        List<String> linhas = new ArrayList<>();
        for (EstadoProcesso estado : EstadoProcesso.values()) {
            for (AcaoProcesso acao : AcaoProcesso.values()) {
                try {
                    EstadoProcesso acima = tipo.fluxo().proximo(estado, acao, ACIMA);
                    EstadoProcesso ate = tipo.fluxo().proximo(estado, acao, ATE);
                    String destino = acima == ate ? acima.name() : "acima: " + acima + " | até: " + ate;
                    linhas.add(estado + " " + acao + " -> " + destino);
                } catch (TransicaoInvalidaException e) {
                    // fora da tabela: não aparece na lista
                }
            }
        }
        return linhas;
    }

    private static String ordenar(List<String> linhas) {
        return String.join("\n", linhas.stream().map(String::strip).filter(l -> !l.isEmpty()).sorted().toList());
    }
}
