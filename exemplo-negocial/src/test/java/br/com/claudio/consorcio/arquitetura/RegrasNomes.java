package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** NOM: o nome diz onde a classe mora. Dá para achar qualquer coisa sem abrir o código. */
final class RegrasNomes {

    private static final String RENOMEIE = " Corrija: mova a classe para a pasta certa ou troque o sufixo do nome";

    private RegrasNomes() {
    }

    @ArchTest
    static final ArchRule nom001UseCases = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().resideInAPackage(Raiz.emQualquer("application"))
            .as("NOM-001: *USECASE FICA EM APPLICATION")
            .because("o sufixo diz a camada." + RENOMEIE);

    @ArchTest
    static final ArchRule nom002Resources = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Resource")
            .should().resideInAPackage(Raiz.emQualquer("entrypoint"))
            .as("NOM-002: *RESOURCE FICA EM ENTRYPOINT")
            .because("o sufixo diz a camada." + RENOMEIE);

    @ArchTest
    static final ArchRule nom003Assemblers = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Assembler")
            .should().resideInAPackage(Raiz.emQualquer("entrypoint"))
            .allowEmptyShould(true)
            .as("NOM-003: *ASSEMBLER FICA EM ENTRYPOINT")
            .because("montar a resposta do canal é papel do entrypoint." + RENOMEIE);

    @ArchTest
    static final ArchRule nom004Gateways = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Gateway")
            .should().resideInAPackage(Raiz.pacote("gateway"))
            .allowEmptyShould(true)
            .as("NOM-004: *GATEWAY FICA EM GATEWAY")
            .because("Gateway é o acesso a sistema externo." + RENOMEIE);

    @ArchTest
    static final ArchRule nom005Entidades = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage(Raiz.pacote("persistence"))
            .as("NOM-005: *ENTITY FICA EM PERSISTENCE")
            .because("Entity é o formato da tabela." + RENOMEIE);

    @ArchTest
    static final ArchRule nom006MappersEAdaptersDeRepositorio = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Mapper").or().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().resideInAPackage(Raiz.pacote("persistence"))
            .allowEmptyShould(true)
            .as("NOM-006: *MAPPER E *REPOSITORYADAPTER FICAM EM PERSISTENCE")
            .because("eles convertem entre domínio e banco." + RENOMEIE);

    @ArchTest
    static final ArchRule nom007PadroesDeApresentacao = ArchRuleDefinition.classes()
            .that().haveSimpleNameStartingWith("Padrao")
            .should().resideInAPackage(Raiz.pacote("entrypoint"))
            .allowEmptyShould(true)
            .as("NOM-007: PADRAO* FICA EM ENTRYPOINT")
            .because("padrão de apresentação é por canal." + RENOMEIE);
}
