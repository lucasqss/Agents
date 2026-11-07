package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.internoDoGateway;
import static br.com.claudio.consorcio.arquitetura.Raiz.emQualquer;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** O que é compartilhado: entidades do nosso banco, gateways de sistemas externos e apresentação. */
final class RegrasShared {

    private RegrasShared() {
    }

    @ArchTest
    static final ArchRule sharedNaoConheceOsLados = noClasses()
            .that().resideInAPackage(pacote("shared"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("command"), pacote("query"))
            .because("shared é usado por command e query, nunca o contrário; por isso um gateway nunca importa o domínio");

    @ArchTest
    static final ArchRule entidadesSemComportamento = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(pacote("shared.persistence"))
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveNameMatching("(get|set|is)[A-Z].*")
            .because("regra de negócio fica no domínio, não nas entidades JPA");

    @ArchTest
    static final ArchRule sistemasExternosSoViaGateway = noClasses()
            .that().resideOutsideOfPackage(pacote("shared.gateway"))
            .should().dependOnClassesThat(internoDoGateway())
            .because("client e DTOs externos não saem do pacote do gateway; fora dele só *Gateway e *Exception");

    @ArchTest
    static final ArchRule gatewaySoNosUseCases = noClasses()
            .that().resideOutsideOfPackages(
                    pacote("shared.gateway"), emQualquer("application"), emQualquer("entrypoint"), pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(pacote("shared.gateway"))
            .because("só use cases chamam sistemas externos; o entrypoint só traduz a exceção para HTTP");

    @ArchTest
    static final ArchRule sistemasExternosIndependentes = slices()
            .matching(pacote("shared.gateway.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .because("cada sistema externo tem o próprio gateway; quem combina dois sistemas é o use case");
}
