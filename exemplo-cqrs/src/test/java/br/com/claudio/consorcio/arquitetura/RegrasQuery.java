package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
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
            .that().resideInAPackage(pacote("query"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command"))
            .because("query nunca importa command (CQRS)");

    @ArchTest
    static final ArchRule persistenciaDeLeituraNaBase = noClasses()
            .that().resideInAPackage(pacote("query.persistence"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("query.application"), pacote("query.entrypoint"))
            .because("a direção é entrypoint -> application -> persistence");

    @ArchTest
    static final ArchRule entrypointNaoUsaRepositorio = noClasses()
            .that().resideInAPackage(pacote("query.entrypoint"))
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .because("o entrypoint chama use cases; dos repositórios ele só vê os records projetados");
}
