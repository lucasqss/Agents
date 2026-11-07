package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Domain Model puro (PoEAA): Java sem framework e sem conhecer as outras camadas. */
final class RegrasDominio {

    private RegrasDominio() {
    }

    @ArchTest
    static final ArchRule dominioPuro = noClasses()
            .that().resideInAPackage(pacote("command.domain"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "jakarta.data..",
                    "jakarta.enterprise..",
                    "jakarta.inject..",
                    "jakarta.transaction..",
                    "jakarta.ws.rs..",
                    "jakarta.validation..",
                    "io.quarkus..",
                    "org.hibernate..",
                    "org.eclipse.microprofile..",
                    pacote("shared"),
                    pacote("command.application"),
                    pacote("command.persistence"),
                    pacote("command.entrypoint"),
                    pacote("command.gateway"))
            .because("o domínio não conhece framework, banco, HTTP, sistemas externos nem as outras camadas");
}
