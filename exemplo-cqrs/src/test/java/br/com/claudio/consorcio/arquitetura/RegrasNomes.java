package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.emQualquer;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** NOM: o nome diz onde a classe mora. Dá para achar qualquer coisa sem abrir o código. */
final class RegrasNomes {

    private static final String RENOMEIE = " Corrija: mova a classe para a pasta certa ou troque o sufixo do nome";

    private RegrasNomes() {
    }

    @ArchTest
    static final ArchRule nom001UseCases = classes()
            .that().haveSimpleNameEndingWith("UseCase")
            .should().resideInAPackage(emQualquer("application"))
            .as("NOM-001: *USECASE FICA EM APPLICATION")
            .because("o sufixo diz a camada." + RENOMEIE);

    @ArchTest
    static final ArchRule nom002Resources = classes()
            .that().haveSimpleNameEndingWith("Resource")
            .should().resideInAPackage(emQualquer("entrypoint"))
            .as("NOM-002: *RESOURCE FICA EM ENTRYPOINT")
            .because("o sufixo diz a camada." + RENOMEIE);

    @ArchTest
    static final ArchRule nom003Assemblers = classes()
            .that().haveSimpleNameEndingWith("Assembler")
            .should().resideInAPackage(emQualquer("entrypoint"))
            .allowEmptyShould(true)
            .as("NOM-003: *ASSEMBLER FICA EM ENTRYPOINT")
            .because("montar a resposta do canal é papel do entrypoint." + RENOMEIE);

    @ArchTest
    static final ArchRule nom004Gateways = classes()
            .that().haveSimpleNameEndingWith("Gateway")
            .should().resideInAnyPackage(pacote("shared.gateway"), pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("NOM-004: *GATEWAY FICA EM SHARED.GATEWAY OU COMMAND.GATEWAY")
            .because("Gateway é o acesso a sistema externo." + RENOMEIE);

    @ArchTest
    static final ArchRule nom005Entidades = classes()
            .that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage(pacote("shared.persistence"))
            .as("NOM-005: *ENTITY FICA EM SHARED.PERSISTENCE")
            .because("Entity é o formato da tabela." + RENOMEIE);

    @ArchTest
    static final ArchRule nom006MappersEAdaptersDeRepositorio = classes()
            .that().haveSimpleNameEndingWith("Mapper").or().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().resideInAPackage(pacote("command.persistence"))
            .allowEmptyShould(true)
            .as("NOM-006: *MAPPER E *REPOSITORYADAPTER FICAM EM COMMAND.PERSISTENCE")
            .because("eles convertem entre domínio e banco do lado de escrita." + RENOMEIE);

    @ArchTest
    static final ArchRule nom007PadroesDeApresentacao = classes()
            .that().haveSimpleNameStartingWith("Padrao")
            .should().resideInAPackage(pacote("shared.apresentacao"))
            .allowEmptyShould(true)
            .as("NOM-007: PADRAO* FICA EM SHARED.APRESENTACAO")
            .because("padrão de apresentação é por canal e compartilhado." + RENOMEIE);
}
