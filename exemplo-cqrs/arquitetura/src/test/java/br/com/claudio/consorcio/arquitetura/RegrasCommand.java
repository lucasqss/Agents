package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * CMD: lado de escrita. Service Layer + Domain Model, com o banco atrás das interfaces do domínio.
 * <p>ID vago: CMD-004 (entrypoint de escrita sem banco), hoje CAN-011, porque o HTTP mudou para os canais.
 */
final class RegrasCommand {

    private RegrasCommand() {
    }

    @ArchTest
    static final ArchRule cmd001EscritaIndependenteDaLeitura = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("command"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("query"))
            .as("CMD-001: COMMAND NÃO PODE DEPENDER DE QUERY")
            .because("a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica."
                    + " Corrija: busque pelo repositório do domínio (command.domain) e, se faltar método, crie-o lá");

    @ArchTest
    static final ArchRule cmd002UseCaseSemImplementacaoDeRepositorio = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("command.application"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.persistence"))
            .as("CMD-002: USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO")
            .because("o use case vê o banco só pela interface do domínio (Separated Interface)."
                    + " Corrija: injete a interface de command.domain (ex.: CotaRepository), não o *RepositoryAdapter");

    @ArchTest
    static final ArchRule cmd003UseCaseSemEntidadeJpa = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("command.application"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    Raiz.pacote("shared.persistence"), "jakarta.persistence..", "jakarta.data..", "io.quarkus.hibernate..")
            .as("CMD-003: USE CASE NÃO PODE USAR ENTIDADE JPA NEM JAKARTA DATA")
            .because("o use case trabalha com objetos do domínio."
                    + " Corrija: use o objeto do domínio; a conversão entidade <-> domínio é do Mapper em command.persistence");

    @ArchTest
    static final ArchRule cmd005PersistenciaSoPelaInterface = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("command.persistence"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.persistence"))
            .as("CMD-005: COMMAND.PERSISTENCE SÓ É USADO PELA INTERFACE DO DOMÍNIO")
            .because("a implementação dos repositórios é encontrada via CDI pela interface do domínio."
                    + " Corrija: dependa da interface em command.domain, nunca da classe de command.persistence");

    /*
     * command.gateway ainda não existe: ele nasce quando uma regra do DOMÍNIO precisar de um sistema externo.
     * O domínio declara a interface; command.gateway a implementa delegando ao Gateway de shared.gateway.
     */

    @ArchTest
    static final ArchRule cmd006PortaDoDominioParaSistemaExterno = ArchRuleDefinition.classes()
            .that(Condicoes.implementaInterfaceDoDominio()).and(Condicoes.usamGateway())
            .should().resideInAPackage(Raiz.pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("CMD-006: IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO FICA EM COMMAND.GATEWAY")
            .because("cada pasta tem um papel só."
                    + " Corrija: mova a classe para command.gateway");

    @ArchTest
    static final ArchRule cmd007CommandGatewaySoTraduz = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    Raiz.pacote("command.application"), Raiz.pacote("command.persistence"))
            .allowEmptyShould(true)
            .as("CMD-007: COMMAND.GATEWAY SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO")
            .because("ele não orquestra nem acessa banco."
                    + " Corrija: deixe só a tradução domínio <-> Gateway; orquestração vai para o use case");

    @ArchTest
    static final ArchRule cmd008CommandGatewaySoPelaInterface = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("CMD-008: COMMAND.GATEWAY SÓ É USADO PELA INTERFACE DO DOMÍNIO")
            .because("quem precisa usa a interface, nunca a implementação."
                    + " Corrija: injete a interface declarada em command.domain");

    @ArchTest
    static final ArchRule cmd009ApplicationAgrupadoPorTipo = ArchRuleDefinition.classes()
            .that().resideInAPackage(Raiz.pacote("command.application"))
            .and().doNotHaveSimpleName("package-info")
            .should().resideInAnyPackage(
                    Raiz.pacote("command.application.usecase"),
                    Raiz.pacote("command.application.exception"),
                    Raiz.pacote("command.application.dto"))
            .as("CMD-009: COMMAND.APPLICATION AGRUPADO POR TIPO (USECASE, EXCEPTION, DTO)")
            .because("cada tipo de classe tem lugar fixo."
                    + " Corrija: *UseCase em usecase, exceção em exception, dados de entrada do use case em dto");

    @ArchTest
    static final ArchRule cmd010PersistenceAgrupadoPorTipo = ArchRuleDefinition.classes()
            .that().resideInAPackage(Raiz.pacote("command.persistence"))
            .and().doNotHaveSimpleName("package-info")
            .should().resideInAnyPackage(
                    Raiz.pacote("command.persistence.repository"),
                    Raiz.pacote("command.persistence.mapper"),
                    Raiz.pacote("command.persistence.adapter"))
            .as("CMD-010: COMMAND.PERSISTENCE AGRUPADO POR TIPO (REPOSITORY, MAPPER, ADAPTER)")
            .because("cada tipo de classe tem lugar fixo."
                    + " Corrija: *DataRepository em repository, *Mapper em mapper, *RepositoryAdapter em adapter");

    @ArchTest
    static final ArchRule cmd011MapperSoNoAdapter = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages(
                    Raiz.pacote("command.persistence.adapter"), Raiz.pacote("command.persistence.mapper"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.persistence.mapper"))
            .as("CMD-011: MAPPER SÓ É USADO PELO ADAPTER")
            .because("o mapper é detalhe de como o adapter grava; ele só é público por estar em outro pacote."
                    + " Corrija: chame a interface de repositório do domínio; a conversão fica no *RepositoryAdapter");

    @ArchTest
    static final ArchRule cmd012RepositorioJakartaDataSoNoAdapter = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages(
                    Raiz.pacote("command.persistence.adapter"), Raiz.pacote("command.persistence.repository"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.persistence.repository"))
            .as("CMD-012: REPOSITÓRIO JAKARTA DATA DA ESCRITA SÓ É USADO PELO ADAPTER")
            .because("o *DataRepository trabalha com *Entity; o resto da escrita trabalha com o domínio."
                    + " Corrija: chame a interface de repositório do domínio; o *RepositoryAdapter usa o *DataRepository");
}
