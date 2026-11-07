package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.data.repository.Repository;
import jakarta.persistence.Entity;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.jboss.resteasy.reactive.server.ServerExceptionMapper;

/** Onde cada tecnologia pode aparecer. */
final class RegrasTecnologia {

    private RegrasTecnologia() {
    }

    @ArchTest
    static final ArchRule entidadesJpaEmSharedPersistence = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..shared.persistence..")
            .because("as entidades são o modelo do nosso banco, compartilhado por escrita e leitura");

    @ArchTest
    static final ArchRule panacheSoNaEscrita = noClasses()
            .that().resideOutsideOfPackage("..command.persistence..")
            .should().dependOnClassesThat().resideInAPackage("io.quarkus.hibernate.orm.panache..")
            .because("Panache é a implementação dos repositórios do domínio");

    @ArchTest
    static final ArchRule jakartaDataSoNaLeitura = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAPackage("..query.persistence..")
            .because("Jakarta Data com @Query HQL é a persistência da leitura");

    @ArchTest
    static final ArchRule restClientSoNoGateway = classes()
            .that().areAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage("..shared.gateway..")
            .because("chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway");

    @ArchTest
    static final ArchRule resilienciaSoNoGateway = noClasses()
            .that().resideOutsideOfPackage("..shared.gateway..")
            .should().dependOnClassesThat().resideInAPackage("org.eclipse.microprofile.faulttolerance..")
            .because("timeout, retry e circuit breaker protegem chamadas a sistemas externos");

    @ArchTest
    static final ArchRule endpointsSoNoEntrypoint = classes()
            .that().areAnnotatedWith(Path.class).and().areNotAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage("..entrypoint..")
            .because("só o entrypoint expõe HTTP");

    @ArchTest
    static final ArchRule validacaoSoNoEntrypoint = noClasses()
            .that().resideOutsideOfPackage("..entrypoint..")
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Bean Validation é o porteiro do formato da entrada; regra de negócio fica no domínio");

    @ArchTest
    static final ArchRule exceptionMappersSoNoEntrypoint = methods()
            .that().areAnnotatedWith(ServerExceptionMapper.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..entrypoint..")
            .because("traduzir exceção para HTTP é papel do entrypoint");

    @ArchTest
    static final ArchRule transacaoSoNoUseCase = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..application..")
            .allowEmptyShould(true)
            .because("o use case (Service Layer) define a fronteira da transação");

    @ArchTest
    static final ArchRule transacaoDeClasseSoNoUseCase = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage("..application..")
            .allowEmptyShould(true)
            .because("o use case (Service Layer) define a fronteira da transação");
}
