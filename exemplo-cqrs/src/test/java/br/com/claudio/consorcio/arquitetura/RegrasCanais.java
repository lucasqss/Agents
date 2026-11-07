package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.naoRetornarTipoDePersistence;
import static br.com.claudio.consorcio.arquitetura.Condicoes.usadoSoPeloProprioCanal;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.data.repository.Repository;

/**
 * Canais de atendimento (web, mobile, TAA...): um BFF por canal em entrypoint.&lt;canal&gt; e um padrão de
 * apresentação por canal em shared.apresentacao.&lt;canal&gt;. Nenhuma regra lista os canais: o nome do
 * subpacote é o canal, e um canal novo herda tudo.
 */
final class RegrasCanais {

    private RegrasCanais() {
    }

    @ArchTest
    static final ArchRule canaisNaoSeConhecem = slices()
            .matching("..entrypoint.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .because("cada canal tem o próprio contrato; nenhum reaproveita o de outro");

    @ArchTest
    static final ArchRule padroesNaoSeConhecem = slices()
            .matching("..shared.apresentacao.(*)..")
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .because("o que é comum aos canais fica na base (Formatador, Rotulos)");

    @ArchTest
    static final ArchRule padraoDoCanalSoNoProprioCanal = classes()
            .that().resideInAPackage("..shared.apresentacao.*..")
            .should(usadoSoPeloProprioCanal())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule apresentacaoSoNoEntrypoint = noClasses()
            .that().resideOutsideOfPackages("..entrypoint..", "..shared.apresentacao..")
            .should().dependOnClassesThat().resideInAPackage("..shared.apresentacao..")
            .because("formatação é assunto do entrypoint; use case e consulta não formatam nada");

    @ArchTest
    static final ArchRule baseComumSemCanal = noClasses()
            .that().resideInAPackage("..shared.apresentacao")
            .should().dependOnClassesThat().resideInAPackage("..shared.apresentacao.*..")
            .because("Formatador e Rotulos são a base que todos os canais usam");

    @ArchTest
    static final ArchRule useCasesSemCanal = noClasses()
            .that().resideInAPackage("..application..")
            .should().dependOnClassesThat().resideInAPackage("..entrypoint..")
            .because("o mesmo use case atende todos os canais");

    @ArchTest
    static final ArchRule contratoDoCanalIsolado = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..entrypoint.*..")
            .and().arePublic()
            .should(naoRetornarTipoDePersistence())
            .allowEmptyShould(true)
            .because("o canal devolve o próprio Response; mudar uma projeção não pode mudar o JSON do front");

    @ArchTest
    static final ArchRule apresentacaoPura = noClasses()
            .that().haveSimpleNameEndingWith("Assembler").or().resideInAPackage("..shared.apresentacao..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..", "jakarta.inject..", "jakarta.persistence..", "io.quarkus..")
            .orShould().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .because("Assembler e padrões de canal são funções puras, testáveis sem Quarkus nem banco");
}
