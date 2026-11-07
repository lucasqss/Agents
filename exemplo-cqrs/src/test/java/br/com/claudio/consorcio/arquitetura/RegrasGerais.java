package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Estrutura geral do projeto. */
final class RegrasGerais {

    private RegrasGerais() {
    }

    /**
     * Todo código fica em command, query ou shared. Esta regra NUNCA passa vazia: se a pasta foi copiada
     * para o pacote errado (e nenhuma classe foi encontrada), ela falha em vez de deixar tudo passar.
     */
    @ArchTest
    static final ArchRule estrutura = classes()
            .should().resideInAnyPackage(pacote("command"), pacote("query"), pacote("shared"))
            .allowEmptyShould(false)
            .because("a raiz do projeto só tem command (escrita), query (leitura) e shared");

    /** Sem ciclos entre os pacotes de segundo nível (command.domain, query.persistence, shared.gateway...). */
    @ArchTest
    static final ArchRule semCiclos = slices()
            .matching(Raiz.PACOTE + ".(*).(*)..")
            .should().beFreeOfCycles();
}
