package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.emQualquer;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
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
            .should().resideInAPackage(pacote("shared.persistence"))
            .because("as entidades são o modelo do nosso banco, compartilhado por escrita e leitura");

    @ArchTest
    static final ArchRule semPanache = noClasses()
            .should().dependOnClassesThat().resideInAPackage("io.quarkus.hibernate.orm.panache..")
            .because("a persistência usa só Jakarta Data, com HQL validado na compilação");

    @ArchTest
    static final ArchRule jakartaDataSoEmPersistence = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAnyPackage(pacote("command.persistence"), pacote("query.persistence"))
            .because("repositórios Jakarta Data são a persistência de cada lado");

    @ArchTest
    static final ArchRule restClientSoNoGateway = classes()
            .that().areAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(pacote("shared.gateway"))
            .because("chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway");

    @ArchTest
    static final ArchRule resilienciaSoNoGateway = noClasses()
            .that().resideOutsideOfPackage(pacote("shared.gateway"))
            .should().dependOnClassesThat().resideInAPackage("org.eclipse.microprofile.faulttolerance..")
            .because("timeout, retry e circuit breaker protegem chamadas a sistemas externos");

    @ArchTest
    static final ArchRule endpointsSoNoEntrypoint = classes()
            .that().areAnnotatedWith(Path.class).and().areNotAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(emQualquer("entrypoint"))
            .because("só o entrypoint expõe HTTP");

    @ArchTest
    static final ArchRule validacaoSoNoEntrypoint = noClasses()
            .that().resideOutsideOfPackage(emQualquer("entrypoint"))
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .because("Bean Validation é o porteiro do formato da entrada; regra de negócio fica no domínio");

    @ArchTest
    static final ArchRule exceptionMappersSoNoEntrypoint = methods()
            .that().areAnnotatedWith(ServerExceptionMapper.class)
            .should().beDeclaredInClassesThat().resideInAPackage(emQualquer("entrypoint"))
            .because("traduzir exceção para HTTP é papel do entrypoint");

    @ArchTest
    static final ArchRule transacaoSoNoUseCase = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage(emQualquer("application"))
            .allowEmptyShould(true)
            .because("o use case (Service Layer) define a fronteira da transação");

    @ArchTest
    static final ArchRule transacaoDeClasseSoNoUseCase = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage(emQualquer("application"))
            .allowEmptyShould(true)
            .because("o use case (Service Layer) define a fronteira da transação");
}
