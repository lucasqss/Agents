package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "br.com.claudio.consorcio", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    /** Regra 1: o domínio de escrita é Java puro e não conhece nenhuma outra camada. */
    @ArchTest
    static final ArchRule dominioPuro = noClasses()
            .that().resideInAPackage("..command.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "io.quarkus..",
                    "org.hibernate..",
                    "..shared..",
                    "..command.application..",
                    "..command.persistence..")
            .because("o domínio não pode conhecer JPA, Quarkus, Hibernate, integrações nem as outras camadas");

    /** Regra 2: entidades JPA só têm getters e setters. */
    @ArchTest
    static final ArchRule entidadesSemComportamento = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..shared.persistence..")
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveNameMatching("(get|set|is)[A-Z].*")
            .because("regra de negócio fica no domínio, não nas entidades JPA");

    /** Regra 3: os use cases de escrita só enxergam o banco pelas interfaces de repositório do domínio. */
    @ArchTest
    static final ArchRule useCasesSemBanco = noClasses()
            .that().resideInAPackage("..command.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..command.persistence..",
                    "..shared.persistence..",
                    "jakarta.persistence..",
                    "io.quarkus.hibernate..")
            .because("a persistência do comando fica atrás das interfaces de repositório do domínio");

    /** Regra 4: o lado de consulta é independente do lado de comando. */
    @ArchTest
    static final ArchRule consultaIndependenteDoComando = noClasses()
            .that().resideInAPackage("..query..")
            .should().dependOnClassesThat().resideInAPackage("..command..")
            .because("query não pode importar nada de command");

    /** Regra 5: o que é compartilhado não conhece nenhum dos lados. */
    @ArchTest
    static final ArchRule compartilhadoNaoConheceOsLados = noClasses()
            .that().resideInAPackage("..shared..")
            .should().dependOnClassesThat().resideInAnyPackage("..command..", "..query..")
            .because("shared é usado por command e query, nunca o contrário");

    /** Regra 6: sistemas externos só via Gateway, e só a partir de use cases (ou do mapeamento de erros). */
    @ArchTest
    static final ArchRule integracoesSoViaGateway = noClasses()
            .that().resideOutsideOfPackage("..shared.integracao..")
            .should().dependOnClassesThat(internoDeIntegracao())
            .because("client e DTOs externos não vazam do pacote da integração");

    @ArchTest
    static final ArchRule gatewaysSoNosUseCases = noClasses()
            .that().resideOutsideOfPackages("..shared.integracao..", "..application..", "..entrypoint..")
            .should().dependOnClassesThat().resideInAPackage("..shared.integracao..")
            .because("só use cases chamam sistemas externos; o entrypoint só traduz as exceções");

    /** Regra 7: sem ciclos entre os pacotes. */
    @ArchTest
    static final ArchRule semCiclos = slices()
            .matching("br.com.claudio.consorcio.(*).(*)..")
            .should().beFreeOfCycles();

    private static DescribedPredicate<JavaClass> internoDeIntegracao() {
        return DescribedPredicate.describe("são client ou DTO de integração",
                c -> c.getPackageName().contains(".shared.integracao")
                        && !c.getSimpleName().endsWith("Gateway")
                        && !c.getSimpleName().endsWith("Exception"));
    }
}
