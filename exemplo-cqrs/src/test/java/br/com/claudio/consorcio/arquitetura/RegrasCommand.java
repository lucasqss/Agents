package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.implementaInterfaceDoDominio;
import static br.com.claudio.consorcio.arquitetura.Condicoes.usamGateway;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
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
            .that().resideInAPackage(pacote("command"))
            .should().dependOnClassesThat().resideInAPackage(pacote("query"))
            .because("a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica");

    @ArchTest
    static final ArchRule useCasesSemBanco = noClasses()
            .that().resideInAPackage(pacote("command.application"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    pacote("command.persistence"),
                    pacote("shared.persistence"),
                    "jakarta.persistence..",
                    "io.quarkus.hibernate..")
            .because("o use case vê o banco só pelas interfaces de repositório do domínio (Separated Interface)");

    @ArchTest
    static final ArchRule entrypointSemBanco = noClasses()
            .that().resideInAPackage(pacote("command.entrypoint"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("command.persistence"), pacote("shared.persistence"))
            .because("o entrypoint de escrita fala só com os use cases");

    @ArchTest
    static final ArchRule persistenciaSoPelaInterface = noClasses()
            .that().resideOutsideOfPackage(pacote("command.persistence"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command.persistence"))
            .because("a implementação dos repositórios é encontrada via CDI pela interface do domínio");

    /*
     * command.gateway ainda não existe: ele nasce quando uma regra do DOMÍNIO precisar de um sistema externo.
     * O domínio declara a interface; command.gateway a implementa delegando ao Gateway de shared.gateway.
     */

    @ArchTest
    static final ArchRule portaDoDominioParaSistemaExternoMoraEmCommandGateway = classes()
            .that(implementaInterfaceDoDominio()).and(usamGateway())
            .should().resideInAPackage(pacote("command.gateway"))
            .allowEmptyShould(true)
            .because("a implementação de uma interface do domínio que chama sistema externo fica em command.gateway");

    @ArchTest
    static final ArchRule commandGatewaySoTraduz = noClasses()
            .that().resideInAPackage(pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    pacote("command.application"), pacote("command.persistence"), pacote("command.entrypoint"))
            .allowEmptyShould(true)
            .because("command.gateway só liga a interface do domínio ao Gateway externo");

    @ArchTest
    static final ArchRule commandGatewaySoPelaInterface = noClasses()
            .that().resideOutsideOfPackage(pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command.gateway"))
            .allowEmptyShould(true)
            .because("quem precisa usa a interface do domínio, nunca a implementação");
}
