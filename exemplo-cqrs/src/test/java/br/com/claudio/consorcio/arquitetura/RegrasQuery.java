package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.data.repository.Repository;

/** Lado de leitura: entrypoint -> application -> persistence, sem domínio e sem portas. */
final class RegrasQuery {

    private RegrasQuery() {
    }

    @ArchTest
    static final ArchRule consultaIndependenteDoComando = noClasses()
            .that().resideInAPackage("..query..")
            .should().dependOnClassesThat().resideInAPackage("..command..")
            .because("query nunca importa command (CQRS)");

    @ArchTest
    static final ArchRule persistenciaDeLeituraNaBase = noClasses()
            .that().resideInAPackage("..query.persistence..")
            .should().dependOnClassesThat().resideInAnyPackage("..query.application..", "..query.entrypoint..")
            .because("a direção é entrypoint -> application -> persistence");

    @ArchTest
    static final ArchRule entrypointNaoUsaRepositorio = noClasses()
            .that().resideInAPackage("..query.entrypoint..")
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .because("o entrypoint chama use cases; dos repositórios ele só vê os records projetados");
}
