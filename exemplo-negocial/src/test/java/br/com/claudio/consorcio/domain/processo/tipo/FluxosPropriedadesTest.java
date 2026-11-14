package br.com.claudio.consorcio.domain.processo.tipo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.FluxoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

/**
 * Propriedades da tabela de fluxo de cada tipo, verificadas em caminhos gerados (jqwik): partindo de ABERTO, uma
 * sequência qualquer de ações válidas, com valor e alçada quaisquer. Cobre combinações que os exemplos de
 * {@code FluxoProcessoTest} não enumeram.
 */
class FluxosPropriedadesTest {

    @Property
    void todaAcaoForaDaTabelaERecusadaETodaDaTabelaEAceita(@ForAll TipoProcesso tipo,
            @ForAll EstadoProcesso estado, @ForAll AcaoProcesso acao, @ForAll("contexto") ContextoDaTransicao contexto) {
        FluxoProcesso fluxo = tipo.fluxo();
        if (validas(fluxo, estado).contains(acao)) {
            EstadoProcesso destino = fluxo.proximo(estado, acao, contexto);
            assertTrue(destino != null);
        } else {
            assertThrows(TransicaoInvalidaException.class, () -> fluxo.proximo(estado, acao, contexto));
            assertThrows(TransicaoInvalidaException.class, () -> fluxo.papeisDe(estado, acao));
            assertFalse(fluxo.exigeOutraPessoaQueConcluiuAAnalise(estado, acao));
        }
    }

    @Property
    void estadoFinalNaoTemSaidaEOsDemaisTem(@ForAll TipoProcesso tipo, @ForAll EstadoProcesso estado) {
        assertEquals(estado.finalizado(), validas(tipo.fluxo(), estado).isEmpty(), estado::name);
    }

    @Property
    void cancelarValeEmTodoEstadoAbertoMenosComPagamentoSolicitado(@ForAll TipoProcesso tipo,
            @ForAll EstadoProcesso estado) {
        boolean esperado = !estado.finalizado() && estado != EstadoProcesso.PAGAMENTO_SOLICITADO;
        assertEquals(esperado, validas(tipo.fluxo(), estado).contains(AcaoProcesso.CANCELAR), estado::name);
    }

    @Property
    void acaoDaRotinaNaoTemPapelEAcaoDePessoaTem(@ForAll TipoProcesso tipo, @ForAll EstadoProcesso estado) {
        FluxoProcesso fluxo = tipo.fluxo();
        Set<AcaoProcesso> daPessoa = EnumSet.noneOf(AcaoProcesso.class);
        for (AcaoProcesso acao : validas(fluxo, estado)) {
            assertEquals(acao.daRotina(), fluxo.papeisDe(estado, acao).isEmpty(), () -> estado + "/" + acao);
            if (!acao.daRotina()) {
                daPessoa.add(acao);
            }
        }
        assertEquals(daPessoa, fluxo.acoesDisponiveis(estado));
    }

    /** R16: com o valor dentro da alçada, nenhum caminho passa por Aguardando alçada. */
    @Property
    void dentroDaAlcadaNuncaAguardaAlcada(@ForAll TipoProcesso tipo, @ForAll("dentroDaAlcada") ContextoDaTransicao contexto,
            @ForAll @Size(max = 60) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        for (Passo passo : caminhar(tipo.fluxo(), contexto, escolhas)) {
            assertNotEquals(EstadoProcesso.AGUARDANDO_ALCADA, passo.para(), passo::toString);
        }
    }

    /**
     * R16 e R18: acima da alçada, todo caminho que chega à solicitação de pagamento passou antes por uma aprovação na
     * alçada, ação que só o aprovador pede.
     */
    @Property
    void acimaDaAlcadaSoPagaDepoisDaAprovacaoNaAlcada(@ForAll TipoProcesso tipo,
            @ForAll("acimaDaAlcada") ContextoDaTransicao contexto,
            @ForAll @Size(max = 60) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        boolean aprovadoNaAlcada = false;
        for (Passo passo : caminhar(tipo.fluxo(), contexto, escolhas)) {
            if (passo.acao() == AcaoProcesso.APROVAR_NA_ALCADA) {
                aprovadoNaAlcada = true;
                assertEquals(Set.of(Papel.APROVADOR),
                        tipo.fluxo().papeisDe(passo.de(), passo.acao()));
            }
            if (passo.para() == EstadoProcesso.PAGAMENTO_SOLICITADO) {
                assertTrue(aprovadoNaAlcada, passo::toString);
            }
            if (passo.para() == EstadoProcesso.EM_ANALISE) {
                aprovadoNaAlcada = false;
            }
        }
    }

    private record Passo(EstadoProcesso de, AcaoProcesso acao, EstadoProcesso para) {
    }

    /** Caminho a partir de ABERTO: cada escolha seleciona uma das ações válidas no estado em que o processo está. */
    private static List<Passo> caminhar(FluxoProcesso fluxo, ContextoDaTransicao contexto, List<Integer> escolhas) {
        List<Passo> caminho = new ArrayList<>();
        EstadoProcesso atual = EstadoProcesso.ABERTO;
        for (int escolha : escolhas) {
            List<AcaoProcesso> validas = new ArrayList<>(validas(fluxo, atual));
            if (validas.isEmpty()) {
                break;
            }
            AcaoProcesso acao = validas.get(escolha % validas.size());
            EstadoProcesso proximo = fluxo.proximo(atual, acao, contexto);
            caminho.add(new Passo(atual, acao, proximo));
            atual = proximo;
        }
        return caminho;
    }

    private static Set<AcaoProcesso> validas(FluxoProcesso fluxo, EstadoProcesso estado) {
        Set<AcaoProcesso> validas = EnumSet.noneOf(AcaoProcesso.class);
        for (AcaoProcesso acao : AcaoProcesso.values()) {
            try {
                fluxo.papeisDe(estado, acao);
                validas.add(acao);
            } catch (TransicaoInvalidaException naoVale) {
                // fora da tabela neste estado
            }
        }
        return validas;
    }

    private static Arbitrary<Dinheiro> dinheiro() {
        return Arbitraries.longs().between(1, 1_000_000_00L).map(c -> Dinheiro.de(BigDecimal.valueOf(c, 2)));
    }

    @Provide
    Arbitrary<ContextoDaTransicao> contexto() {
        return dinheiro().tuple2().map(t -> new ContextoDaTransicao(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<ContextoDaTransicao> dentroDaAlcada() {
        return dinheiro().tuple2().map(t -> t.get1().maiorQue(t.get2())
                ? new ContextoDaTransicao(t.get2(), t.get1())
                : new ContextoDaTransicao(t.get1(), t.get2()));
    }

    @Provide
    Arbitrary<ContextoDaTransicao> acimaDaAlcada() {
        return dinheiro().tuple2().filter(t -> !t.get1().equals(t.get2()))
                .map(t -> t.get1().maiorQue(t.get2())
                        ? new ContextoDaTransicao(t.get1(), t.get2())
                        : new ContextoDaTransicao(t.get2(), t.get1()));
    }
}
