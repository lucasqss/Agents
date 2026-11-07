package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.implementaInterfaceDoDominio;
import static br.com.claudio.consorcio.arquitetura.Condicoes.usamGateway;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** CMD: lado de escrita. Service Layer + Domain Model, com o banco atrás das interfaces do domínio. */
final class RegrasCommand {

    private RegrasCommand() {
    }

    @ArchTest
    static final ArchRule cmd001EscritaIndependenteDaLeitura = noClasses()
            .that().resideInAPackage(pacote("command"))
            .should().dependOnClassesThat().resideInAPackage(pacote("query"))
            .as("CMD-001: COMMAND NÃO PODE DEPENDER DE QUERY")
            .because("a escrita decide com o domínio; projeções do query mudam conforme as telas e podem estar numa réplica."
                    + " Corrija: busque pelo repositório do domínio (command.domain) e, se faltar método, crie-o lá");

    @ArchTest
    static final ArchRule cmd002UseCaseSemImplementacaoDeRepositorio = noClasses()
            .that().resideInAPackage(pacote("command.application"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command.persistence"))
            .as("CMD-002: USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO")
            .because("o use case vê o banco só pela interface do domínio (Separated Interface)."
                    + " Corrija: injete a interface de command.domain (ex.: CotaRepository), não o *RepositoryAdapter");

    @ArchTest
    static final ArchRule cmd003UseCaseSemEntidadeJpa = noClasses()
            .that().resideInAPackage(pacote("command.application"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    pacote("shared.persistence"), "jakarta.persistence..", "jakarta.data..", "io.quarkus.hibernate..")
            .as("CMD-003: USE CASE NÃO PODE USAR ENTIDADE JPA NEM JAKARTA DATA")
            .because("o use case trabalha com objetos do domínio."
                    + " Corrija: use o objeto do domínio; a conversão entidade <-> domínio é do Mapper em command.persistence");

    @ArchTest
    static final ArchRule cmd004EntrypointSemBanco = noClasses()
            .that().resideInAPackage(pacote("command.entrypoint"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("command.persistence"), pacote("shared.persistence"))
            .as("CMD-004: ENTRYPOINT DE ESCRITA NÃO PODE ACESSAR O BANCO")
            .because("o entrypoint só traduz HTTP e chama use case."
                    + " Corrija: crie ou use um use case em command.application");

    @ArchTest
    static final ArchRule cmd005PersistenciaSoPelaInterface = noClasses()
            .that().resideOutsideOfPackage(pacote("command.persistence"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command.persistence"))
            .as("CMD-005: COMMAND.PERSISTENCE SÓ É USADO PELA INTERFACE DO DOMÍNIO")
            .because("a implementação dos repositórios é encontrada via CDI pela interface do domínio."
                    + " Corrija: dependa da interface em command.domain, nunca da classe de command.persistence");

    /*
     * command.gateway ainda não existe: ele nasce quando uma regra do DOMÍNIO precisar de um sistema externo.
     * O domínio declara a interface; command.gateway a implementa delegando ao Gateway de shared.gateway.
     */

    @ArchTest
    static final ArchRule cmd006PortaDoDominioParaSistemaExterno = classes()
            .that(implementaInterfaceDoDominio()).and(usamGateway())
            .should().resideInAPackage(pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("CMD-006: IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO FICA EM COMMAND.GATEWAY")
            .because("cada pasta tem um papel só."
                    + " Corrija: mova a classe para command.gateway");

    @ArchTest
    static final ArchRule cmd007CommandGatewaySoTraduz = noClasses()
            .that().resideInAPackage(pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    pacote("command.application"), pacote("command.persistence"), pacote("command.entrypoint"))
            .allowEmptyShould(true)
            .as("CMD-007: COMMAND.GATEWAY SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO")
            .because("ele não orquestra nem acessa banco."
                    + " Corrija: deixe só a tradução domínio <-> Gateway; orquestração vai para o use case");

    @ArchTest
    static final ArchRule cmd008CommandGatewaySoPelaInterface = noClasses()
            .that().resideOutsideOfPackage(pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("CMD-008: COMMAND.GATEWAY SÓ É USADO PELA INTERFACE DO DOMÍNIO")
            .because("quem precisa usa a interface, nunca a implementação."
                    + " Corrija: injete a interface declarada em command.domain");
}
