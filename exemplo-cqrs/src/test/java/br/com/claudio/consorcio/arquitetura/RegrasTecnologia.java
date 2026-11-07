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

/** TEC: onde cada tecnologia pode aparecer. */
final class RegrasTecnologia {

    private RegrasTecnologia() {
    }

    @ArchTest
    static final ArchRule tec001EntidadesJpaEmSharedPersistence = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(pacote("shared.persistence"))
            .as("TEC-001: @ENTITY SÓ EM SHARED.PERSISTENCE")
            .because("as entidades são o modelo do nosso banco, compartilhado por escrita e leitura."
                    + " Corrija: mova a classe para shared.persistence");

    @ArchTest
    static final ArchRule tec002SemPanache = noClasses()
            .should().dependOnClassesThat().resideInAPackage("io.quarkus.hibernate.orm.panache..")
            .as("TEC-002: PANACHE NÃO É USADO")
            .because("o padrão do projeto é Jakarta Data, com HQL validado na compilação."
                    + " Corrija: crie uma interface @Repository com @Find/@Insert/@Update/@Query");

    @ArchTest
    static final ArchRule tec003JakartaDataSoEmPersistence = classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAnyPackage(pacote("command.persistence"), pacote("query.persistence"))
            .as("TEC-003: @REPOSITORY (JAKARTA DATA) SÓ EM COMMAND.PERSISTENCE OU QUERY.PERSISTENCE")
            .because("repositório Jakarta Data é detalhe de persistência de cada lado."
                    + " Corrija: mova a interface para o persistence do lado que a usa");

    @ArchTest
    static final ArchRule tec004RestClientSoNoGateway = classes()
            .that().areAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(pacote("shared.gateway"))
            .as("TEC-004: REST CLIENT SÓ EM SHARED.GATEWAY")
            .because("chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway."
                    + " Corrija: mova o client para shared.gateway.<sistema> e exponha um *Gateway");

    @ArchTest
    static final ArchRule tec005ResilienciaSoNoGateway = noClasses()
            .that().resideOutsideOfPackage(pacote("shared.gateway"))
            .should().dependOnClassesThat().resideInAPackage("org.eclipse.microprofile.faulttolerance..")
            .as("TEC-005: TIMEOUT, RETRY E CIRCUIT BREAKER SÓ EM SHARED.GATEWAY")
            .because("resiliência protege chamadas a sistemas externos."
                    + " Corrija: coloque @Timeout/@Retry/@CircuitBreaker no client do gateway");

    @ArchTest
    static final ArchRule tec006EndpointsSoNoEntrypoint = classes()
            .that().areAnnotatedWith(Path.class).and().areNotAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(emQualquer("entrypoint"))
            .as("TEC-006: @PATH SÓ NO ENTRYPOINT")
            .because("só o entrypoint expõe HTTP."
                    + " Corrija: mova o *Resource para command.entrypoint ou query.entrypoint.<canal>");

    @ArchTest
    static final ArchRule tec007ValidacaoSoNoEntrypoint = noClasses()
            .that().resideOutsideOfPackage(emQualquer("entrypoint"))
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .as("TEC-007: BEAN VALIDATION SÓ NO ENTRYPOINT")
            .because("Bean Validation é o porteiro do formato da entrada; regra de negócio fica no domínio."
                    + " Corrija: anote o *Request do entrypoint; no domínio, lance DominioException");

    @ArchTest
    static final ArchRule tec008ExceptionMappersSoNoEntrypoint = methods()
            .that().areAnnotatedWith(ServerExceptionMapper.class)
            .should().beDeclaredInClassesThat().resideInAPackage(emQualquer("entrypoint"))
            .as("TEC-008: @SERVEREXCEPTIONMAPPER SÓ NO ENTRYPOINT")
            .because("traduzir exceção para HTTP é papel do entrypoint."
                    + " Corrija: adicione o mapeamento em ExceptionMappers do entrypoint");

    @ArchTest
    static final ArchRule tec009TransacaoDeMetodoSoNoUseCase = methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage(emQualquer("application"))
            .allowEmptyShould(true)
            .as("TEC-009: @TRANSACTIONAL EM MÉTODO SÓ NO USE CASE")
            .because("o use case (Service Layer) define a fronteira da transação."
                    + " Corrija: tire a anotação daqui e coloque no método do *UseCase que chama este código");

    @ArchTest
    static final ArchRule tec010TransacaoDeClasseSoNoUseCase = classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage(emQualquer("application"))
            .allowEmptyShould(true)
            .as("TEC-010: @TRANSACTIONAL EM CLASSE SÓ NO USE CASE")
            .because("o use case (Service Layer) define a fronteira da transação."
                    + " Corrija: tire a anotação da classe e coloque no *UseCase");
}
