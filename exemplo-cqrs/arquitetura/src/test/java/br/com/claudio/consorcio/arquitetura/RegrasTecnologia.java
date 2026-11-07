package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
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
    static final ArchRule tec001EntidadesJpaEmSharedPersistence = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage(Raiz.pacote("shared.persistence"))
            .as("TEC-001: @ENTITY SÓ EM SHARED.PERSISTENCE")
            .because("as entidades são o modelo do nosso banco, compartilhado por escrita e leitura."
                    + " Corrija: mova a classe para shared.persistence");

    @ArchTest
    static final ArchRule tec002SemPanache = ArchRuleDefinition.noClasses()
            .should().dependOnClassesThat().resideInAPackage("io.quarkus.hibernate.orm.panache..")
            .as("TEC-002: PANACHE NÃO É USADO")
            .because("o padrão do projeto é Jakarta Data, com HQL validado na compilação."
                    + " Corrija: crie uma interface @Repository com @Find/@Insert/@Update/@Query");

    @ArchTest
    static final ArchRule tec003JakartaDataSoEmPersistence = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Repository.class)
            .should().resideInAnyPackage(Raiz.pacote("command.persistence"), Raiz.pacote("query.persistence"))
            .as("TEC-003: @REPOSITORY (JAKARTA DATA) SÓ EM COMMAND.PERSISTENCE OU QUERY.PERSISTENCE")
            .because("repositório Jakarta Data é detalhe de persistência de cada lado."
                    + " Corrija: mova a interface para o persistence do lado que a usa");

    @ArchTest
    static final ArchRule tec004RestClientSoNoGateway = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(Raiz.pacote("shared.gateway"))
            .as("TEC-004: REST CLIENT SÓ EM SHARED.GATEWAY")
            .because("chamada HTTP de saída é sempre para um sistema externo, atrás de um Gateway."
                    + " Corrija: mova o client para shared.gateway.<sistema> e exponha um *Gateway");

    @ArchTest
    static final ArchRule tec005ResilienciaSoNoGateway = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("shared.gateway"))
            .should().dependOnClassesThat().resideInAPackage("org.eclipse.microprofile.faulttolerance..")
            .as("TEC-005: TIMEOUT, RETRY E CIRCUIT BREAKER SÓ EM SHARED.GATEWAY")
            .because("resiliência protege chamadas a sistemas externos."
                    + " Corrija: coloque @Timeout/@Retry/@CircuitBreaker no client do gateway");

    @ArchTest
    static final ArchRule tec006EndpointsSoNoEntrypoint = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Path.class).and().areNotAnnotatedWith(RegisterRestClient.class)
            .should().resideInAPackage(Raiz.pacote("canal"))
            .as("TEC-006: @PATH SÓ NOS CANAIS")
            .because("só o módulo de um canal expõe HTTP; o núcleo é biblioteca."
                    + " Corrija: mova o *Resource para canal.<canal>.entrypoint, no módulo do canal");

    @ArchTest
    static final ArchRule tec007ValidacaoSoNoEntrypoint = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("canal"))
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .as("TEC-007: BEAN VALIDATION SÓ NOS CANAIS")
            .because("Bean Validation é o porteiro do formato da entrada de cada canal; regra de negócio fica no domínio."
                    + " Corrija: anote o *Request do canal; no domínio, lance DominioException");

    @ArchTest
    static final ArchRule tec008ExceptionMappersSoNoEntrypoint = ArchRuleDefinition.methods()
            .that().areAnnotatedWith(ServerExceptionMapper.class)
            .should().beDeclaredInClassesThat().resideInAPackage(Raiz.pacote("canal"))
            .as("TEC-008: @SERVEREXCEPTIONMAPPER SÓ NOS CANAIS")
            .because("traduzir exceção para HTTP é papel do canal, igual para todos."
                    + " Corrija: adicione o mapeamento em canal.comum.erro.ExceptionMappers");

    @ArchTest
    static final ArchRule tec009TransacaoDeMetodoSoNoUseCase = ArchRuleDefinition.methods()
            .that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage(Raiz.emQualquer("application"))
            .allowEmptyShould(true)
            .as("TEC-009: @TRANSACTIONAL EM MÉTODO SÓ NO USE CASE")
            .because("o use case (Service Layer) define a fronteira da transação."
                    + " Corrija: tire a anotação daqui e coloque no método do *UseCase que chama este código");

    @ArchTest
    static final ArchRule tec010TransacaoDeClasseSoNoUseCase = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Transactional.class)
            .should().resideInAPackage(Raiz.emQualquer("application"))
            .allowEmptyShould(true)
            .as("TEC-010: @TRANSACTIONAL EM CLASSE SÓ NO USE CASE")
            .because("o use case (Service Layer) define a fronteira da transação."
                    + " Corrija: tire a anotação da classe e coloque no *UseCase");
}
