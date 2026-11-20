package br.com.claudio.pagamento.domain.processo.tipo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.Size;

/**
 * Propriedades dos fluxos verificadas em caminhos gerados (jqwik): partindo de Aberto, uma sequência qualquer de ações
 * válidas, com valor e alçada quaisquer. Cobre combinações de caminho que a {@code TabelaDosFluxosTest} não enumera.
 */
class FluxosPropriedadesTest {

    /** R6: estado final não tem saída; o caminho termina nele. */
    @Property
    void estadoFinalNaoTemSaida(@ForAll TipoProcesso tipo, @ForAll EstadoProcesso estado) {
        if (estado.finalizado()) {
            assertTrue(tipo.fluxo().acoesDisponiveis(estado).isEmpty(), estado::name);
        }
    }

    /** R7: em todo estado que um caminho alcança e que não é final, cancelar vale. */
    @Property
    void todoEstadoAlcancadoNaoFinalPodeSerCancelado(@ForAll TipoProcesso tipo, @ForAll("contexto") ContextoDaTransicao contexto,
            @ForAll @Size(max = 40) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        for (EstadoProcesso estado : caminhar(tipo, contexto, escolhas)) {
            if (!estado.finalizado()) {
                assertTrue(tipo.fluxo().acoesDisponiveis(estado).contains(AcaoProcesso.CANCELAR), estado::name);
            }
        }
    }

    /** R4: com o valor dentro da alçada, nenhum caminho passa por Aguardando alçada. */
    @Property
    void dentroDaAlcadaNuncaAguardaAlcada(@ForAll TipoProcesso tipo, @ForAll("dentroDaAlcada") ContextoDaTransicao contexto,
            @ForAll @Size(max = 40) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        for (EstadoProcesso estado : caminhar(tipo, contexto, escolhas)) {
            assertNotEquals(EstadoProcesso.AGUARDANDO_ALCADA, estado);
        }
    }

    /** R3: o imobiliário só chega a Aprovado passando pela vistoria; o serviço nunca passa por ela. */
    @Property
    void vistoriaSoNoImobiliarioESempreAntesDeAprovar(@ForAll TipoProcesso tipo, @ForAll("contexto") ContextoDaTransicao contexto,
            @ForAll @Size(max = 40) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        boolean passouPelaVistoria = false;
        for (EstadoProcesso estado : caminhar(tipo, contexto, escolhas)) {
            passouPelaVistoria |= estado == EstadoProcesso.AGUARDANDO_VISTORIA;
            if (estado == EstadoProcesso.APROVADO) {
                assertEquals(tipo == TipoProcesso.IMOBILIARIO, passouPelaVistoria);
            }
        }
    }

    /** Pagar só a partir de Aprovado: Pago sempre vem logo depois de Aprovado. */
    @Property
    void pagoSoDepoisDeAprovado(@ForAll TipoProcesso tipo, @ForAll("contexto") ContextoDaTransicao contexto,
            @ForAll @Size(max = 40) List<@IntRange(min = 0, max = 1000) Integer> escolhas) {
        List<EstadoProcesso> caminho = caminhar(tipo, contexto, escolhas);
        for (int i = 1; i < caminho.size(); i++) {
            if (caminho.get(i) == EstadoProcesso.PAGO) {
                assertEquals(EstadoProcesso.APROVADO, caminho.get(i - 1));
            }
        }
    }

    /** Estados visitados a partir de Aberto: cada escolha seleciona uma das ações válidas no estado, em ordem fixa. */
    private static List<EstadoProcesso> caminhar(TipoProcesso tipo, ContextoDaTransicao contexto, List<Integer> escolhas) {
        List<EstadoProcesso> caminho = new ArrayList<>(List.of(EstadoProcesso.ABERTO));
        EstadoProcesso atual = EstadoProcesso.ABERTO;
        for (int escolha : escolhas) {
            List<AcaoProcesso> validas = tipo.fluxo().acoesDisponiveis(atual).stream().sorted().toList();
            if (validas.isEmpty()) {
                break;
            }
            atual = tipo.fluxo().proximo(atual, validas.get(escolha % validas.size()), contexto);
            caminho.add(atual);
        }
        return caminho;
    }

    @Provide
    Arbitrary<ContextoDaTransicao> contexto() {
        return Arbitraries.integers().between(1, 10_000_000).flatMap(alcada -> Arbitraries.integers()
                .between(1, 20_000_000)
                .map(valor -> new ContextoDaTransicao(centavos(valor), centavos(alcada))));
    }

    @Provide
    Arbitrary<ContextoDaTransicao> dentroDaAlcada() {
        return Arbitraries.integers().between(1, 10_000_000).flatMap(alcada -> Arbitraries.integers()
                .between(1, alcada)
                .map(valor -> new ContextoDaTransicao(centavos(valor), centavos(alcada))));
    }

    private static Dinheiro centavos(int centavos) {
        return Dinheiro.de(BigDecimal.valueOf(centavos, 2));
    }
}
