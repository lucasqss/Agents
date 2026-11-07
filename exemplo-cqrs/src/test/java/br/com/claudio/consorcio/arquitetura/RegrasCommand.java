package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.implementaInterfaceDoDominio;
import static br.com.claudio.consorcio.arquitetura.Condicoes.usamGateway;
import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Lado de escrita: Service Layer + Domain Model, com o banco atrás das interfaces do domínio. */
final class RegrasCommand {

    private RegrasCommand() {
    }

    @ArchTest
    static final ArchRule escritaIndependenteDaLeitura = noClasses()
            .that().resideInAPackage("..command..")
            // org.hibernate.query também casa com "..query.."; é a API que o código gerado pelo Jakarta Data usa.
            .should().dependOnClassesThat(resideInAPackage("..query..").and(not(resideInAPackage("org.hibernate.."))))
            .because("a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica");

    @ArchTest
    static final ArchRule useCasesSemBanco = noClasses()
            .that().resideInAPackage("..command.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..command.persistence..",
                    "..shared.persistence..",
                    "jakarta.persistence..",
                    "io.quarkus.hibernate..")
            .because("o use case vê o banco só pelas interfaces de repositório do domínio (Separated Interface)");

    @ArchTest
    static final ArchRule entrypointSemBanco = noClasses()
            .that().resideInAPackage("..command.entrypoint..")
            .should().dependOnClassesThat().resideInAnyPackage("..command.persistence..", "..shared.persistence..")
            .because("o entrypoint de escrita fala só com os use cases");

    @ArchTest
    static final ArchRule persistenciaSoPelaInterface = noClasses()
            .that().resideOutsideOfPackage("..command.persistence..")
            .should().dependOnClassesThat().resideInAPackage("..command.persistence..")
            .because("a implementação dos repositórios é encontrada via CDI pela interface do domínio");

    /*
     * command.gateway ainda não existe: ele nasce quando uma regra do DOMÍNIO precisar de um sistema externo.
     * O domínio declara a interface; command.gateway a implementa delegando ao Gateway de shared.gateway.
     */

    @ArchTest
    static final ArchRule portaDoDominioParaSistemaExternoMoraEmCommandGateway = classes()
            .that(implementaInterfaceDoDominio()).and(usamGateway())
            .should().resideInAPackage("..command.gateway..")
            .allowEmptyShould(true)
            .because("a implementação de uma interface do domínio que chama sistema externo fica em command.gateway");

    @ArchTest
    static final ArchRule commandGatewaySoTraduz = noClasses()
            .that().resideInAPackage("..command.gateway..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..command.application..", "..command.persistence..", "..command.entrypoint..")
            .allowEmptyShould(true)
            .because("command.gateway só liga a interface do domínio ao Gateway externo");

    @ArchTest
    static final ArchRule commandGatewaySoPelaInterface = noClasses()
            .that().resideOutsideOfPackage("..command.gateway..")
            .should().dependOnClassesThat().resideInAPackage("..command.gateway..")
            .allowEmptyShould(true)
            .because("quem precisa usa a interface do domínio, nunca a implementação");
}
