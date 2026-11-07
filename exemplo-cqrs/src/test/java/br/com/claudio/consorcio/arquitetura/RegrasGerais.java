package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** GER: estrutura geral do projeto. */
final class RegrasGerais {

    private RegrasGerais() {
    }

    /**
     * Esta regra NUNCA passa vazia: se a pasta foi copiada para o pacote errado (e nenhuma classe foi
     * encontrada), ela falha em vez de deixar tudo passar.
     */
    @ArchTest
    static final ArchRule ger001Estrutura = classes()
            .should().resideInAnyPackage(pacote("command"), pacote("query"), pacote("shared"))
            .allowEmptyShould(false)
            .as("GER-001: TODO CÓDIGO FICA EM COMMAND, QUERY OU SHARED")
            .because("a raiz do projeto só tem command (escrita), query (leitura) e shared."
                    + " Corrija: mova a classe para o lado certo; se nenhuma classe foi analisada,"
                    + " a pasta arquitetura está no pacote errado (deve ser <raiz>.arquitetura)");

    @ArchTest
    static final ArchRule ger002SemCiclos = slices()
            .matching(Raiz.PACOTE + ".(*).(*)..")
            .should().beFreeOfCycles()
            .as("GER-002: SEM CICLOS ENTRE PACOTES")
            .because("ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra."
                    + " Corrija: inverta a dependência com uma interface ou mova a classe compartilhada");
}
