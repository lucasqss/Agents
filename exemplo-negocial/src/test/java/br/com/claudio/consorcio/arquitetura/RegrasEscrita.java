package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** CMD: casos de uso e escrita. Service Layer + Domain Model, com o banco atrás das interfaces do domínio. */
final class RegrasEscrita {

    private static final String APPLICATION = Raiz.pacote("application");

    private RegrasEscrita() {
    }

    @ArchTest
    static final ArchRule cmd001EscritaNaoUsaConsultaDeTela = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION).and(Condicoes.usamDominio())
            .should().dependOnClassesThat().haveSimpleNameEndingWith("QueryRepository")
            .as("CMD-001: CASO DE USO QUE USA O DOMÍNIO NÃO PODE USAR *QUERYREPOSITORY")
            .because("a escrita decide com o domínio; consultas de tela mudam conforme as telas."
                    + " Corrija: busque pelo repositório em domain/<agregado> e, se faltar método, crie-o lá;"
                    + " se é uma leitura, tire o domínio do caso de uso");

    @ArchTest
    static final ArchRule cmd002UseCaseSemImplementacaoDeRepositorio = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat(Condicoes.implementacaoDaEscrita())
            .as("CMD-002: USE CASE NÃO PODE USAR A IMPLEMENTAÇÃO DO REPOSITÓRIO")
            .because("o use case vê o banco só pela interface do domínio (Separated Interface)."
                    + " Corrija: injete a interface de domain/<agregado> (ex.: CotaRepository), não o *RepositoryAdapter");

    @ArchTest
    static final ArchRule cmd003UseCaseSemEntidadeJpa = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .orShould().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "io.quarkus.hibernate..")
            .as("CMD-003: USE CASE NÃO PODE USAR ENTIDADE JPA")
            .because("o use case trabalha com objetos do domínio ou records de consulta."
                    + " Corrija: use o objeto do domínio; a conversão entidade <-> domínio é do Mapper em persistence/<agregado>");

    @ArchTest
    static final ArchRule cmd004EntrypointSemBanco = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("entrypoint"))
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Entity")
            .orShould().dependOnClassesThat(Condicoes.implementacaoDaEscrita())
            .as("CMD-004: ENTRYPOINT NÃO PODE ACESSAR O BANCO")
            .because("o entrypoint só traduz HTTP e chama use case."
                    + " Corrija: crie ou use um use case em application/<agregado>");

    @ArchTest
    static final ArchRule cmd005PersistenciaSoPelaInterface = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("persistence"))
            .should().dependOnClassesThat(Condicoes.implementacaoDaEscrita())
            .as("CMD-005: A IMPLEMENTAÇÃO DA ESCRITA SÓ É USADA PELA INTERFACE DO DOMÍNIO")
            .because("a implementação dos repositórios é encontrada via CDI pela interface do domínio."
                    + " Corrija: dependa da interface em domain/<agregado>, nunca da classe de persistence/<agregado>");

    @ArchTest
    static final ArchRule cmd006PortaDoDominioParaSistemaExterno = ArchRuleDefinition.classes()
            .that(Condicoes.implementaInterfaceDoDominio()).and(Condicoes.usamGateway())
            .should().resideInAPackage(Raiz.pacote("gateway")).andShould().haveSimpleNameEndingWith("GatewayAdapter")
            .allowEmptyShould(true)
            .as("CMD-006: IMPLEMENTAÇÃO DE INTERFACE DO DOMÍNIO QUE CHAMA SISTEMA EXTERNO É UM *GATEWAYADAPTER EM GATEWAY")
            .because("cada pasta tem um papel só."
                    + " Corrija: mova a classe para gateway/<sistema> com o sufixo GatewayAdapter");

    @ArchTest
    static final ArchRule cmd007GatewayAdapterSoTraduz = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("GatewayAdapter")
            .should().dependOnClassesThat().resideInAnyPackage(
                    APPLICATION, Raiz.pacote("persistence"), Raiz.pacote("entrypoint"))
            .allowEmptyShould(true)
            .as("CMD-007: *GATEWAYADAPTER SÓ LIGA O DOMÍNIO AO GATEWAY EXTERNO")
            .because("ele não orquestra nem acessa banco."
                    + " Corrija: deixe só a tradução domínio <-> Gateway; orquestração vai para o use case");

    @ArchTest
    static final ArchRule cmd008GatewayAdapterSoPelaInterface = ArchRuleDefinition.noClasses()
            .should().dependOnClassesThat().haveSimpleNameEndingWith("GatewayAdapter")
            .allowEmptyShould(true)
            .as("CMD-008: *GATEWAYADAPTER SÓ É USADO PELA INTERFACE DO DOMÍNIO")
            .because("quem precisa usa a interface, nunca a implementação."
                    + " Corrija: injete a interface declarada em domain/<agregado>");

    @ArchTest
    static final ArchRule cmd009UmaOperacaoPorUseCase = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should(Condicoes.terUmUnicoMetodoPublico())
            .as("CMD-009: *USECASE TEM UM ÚNICO MÉTODO PÚBLICO")
            .because("uma classe, uma operação: é o limite que impede classes gigantes."
                    + " Corrija: separe cada operação em seu próprio *UseCase em application/<agregado>");

    @ArchTest
    static final ArchRule cmd010EscritaSemJakartaData = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APPLICATION).and(Condicoes.usamDominio())
            .should().dependOnClassesThat().resideInAPackage("jakarta.data..")
            .as("CMD-010: CASO DE USO QUE USA O DOMÍNIO NÃO PODE USAR JAKARTA DATA")
            .because("na escrita o banco fica atrás da interface do domínio."
                    + " Corrija: declare o método na interface de domain/<agregado> e implemente em persistence/<agregado>");
}
