package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Domain Model puro (PoEAA): Java sem framework e sem conhecer as outras camadas. */
final class RegrasDominio {

    private RegrasDominio() {
    }

    @ArchTest
    static final ArchRule dominioPuro = noClasses()
            .that().resideInAPackage("..command.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "jakarta.data..",
                    "jakarta.enterprise..",
                    "jakarta.inject..",
                    "jakarta.transaction..",
                    "jakarta.ws.rs..",
                    "io.quarkus..",
                    "org.hibernate..",
                    "org.eclipse.microprofile..",
                    "..shared..",
                    "..command.application..",
                    "..command.persistence..",
                    "..command.entrypoint..",
                    "..command.gateway..")
            .because("o domínio não conhece framework, banco, HTTP, sistemas externos nem as outras camadas");
}
