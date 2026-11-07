package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** O nome diz onde a classe mora: dá para achar qualquer coisa sem abrir o código. */
final class RegrasNomes {

    private RegrasNomes() {
    }

    @ArchTest
    static final ArchRule useCases = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().resideInAPackage("..application..");

    @ArchTest
    static final ArchRule resources = classes()
            .that().haveSimpleNameEndingWith("Resource")
            .should().resideInAPackage("..entrypoint..");

    @ArchTest
    static final ArchRule assemblers = classes()
            .that().haveSimpleNameEndingWith("Assembler")
            .should().resideInAPackage("..entrypoint..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule gateways = classes()
            .that().haveSimpleNameEndingWith("Gateway")
            .should().resideInAnyPackage("..shared.gateway..", "..command.gateway..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule entidades = classes()
            .that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage("..shared.persistence..");

    @ArchTest
    static final ArchRule mappersEAdaptersDeRepositorio = classes()
            .that().haveSimpleNameEndingWith("Mapper").or().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().resideInAPackage("..command.persistence..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule padroesDeApresentacao = classes()
            .that().haveSimpleNameStartingWith("Padrao")
            .should().resideInAPackage("..shared.apresentacao..")
            .allowEmptyShould(true);
}
