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
            .should().resideInAPackage(Raiz.emQualquer("application.usecase"))
            .as("NOM-001: *USECASE FICA EM APPLICATION.USECASE")
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
            .should().resideInAnyPackage(Raiz.pacote("shared.gateway"), Raiz.pacote("command.gateway"))
            .allowEmptyShould(true)
            .as("NOM-004: *GATEWAY FICA EM SHARED.GATEWAY OU COMMAND.GATEWAY")
            .because("Gateway é o acesso a sistema externo." + RENOMEIE);

    @ArchTest
    static final ArchRule nom005Entidades = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Entity")
            .should().resideInAPackage(Raiz.pacote("shared.persistence"))
            .as("NOM-005: *ENTITY FICA EM SHARED.PERSISTENCE")
            .because("Entity é o formato da tabela." + RENOMEIE);

    @ArchTest
    static final ArchRule nom006Mappers = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Mapper")
            .should().resideInAPackage(Raiz.pacote("command.persistence.mapper"))
            .allowEmptyShould(true)
            .as("NOM-006: *MAPPER FICA EM COMMAND.PERSISTENCE.MAPPER")
            .because("o mapper converte entre domínio e banco do lado de escrita." + RENOMEIE);

    @ArchTest
    static final ArchRule nom007PadroesDeApresentacao = ArchRuleDefinition.classes()
            .that().haveSimpleNameStartingWith("Padrao")
            .should().resideInAPackage(Raiz.pacote("canal.*.apresentacao"))
            .allowEmptyShould(true)
            .as("NOM-007: PADRAO* FICA EM CANAL.<CANAL>.APRESENTACAO")
            .because("o padrão de apresentação é de um canal (a interface comum fica em canal.comum.apresentacao)." + RENOMEIE);

    @ArchTest
    static final ArchRule nom008MapeadoresDeToken = ArchRuleDefinition.classes()
            .that().haveSimpleNameStartingWith("MapeadorDeToken")
            .should().resideInAPackage(Raiz.pacote("canal.*.seguranca"))
            .allowEmptyShould(true)
            .as("NOM-008: MAPEADORDETOKEN* FICA EM CANAL.<CANAL>.SEGURANCA")
            .because("o token é de um canal, com a biblioteca corporativa dele." + RENOMEIE);

    @ArchTest
    static final ArchRule nom009Adapters = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("RepositoryAdapter")
            .should().resideInAPackage(Raiz.pacote("command.persistence.adapter"))
            .allowEmptyShould(true)
            .as("NOM-009: *REPOSITORYADAPTER FICA EM COMMAND.PERSISTENCE.ADAPTER")
            .because("o adapter implementa a interface de repositório do domínio." + RENOMEIE);

    @ArchTest
    static final ArchRule nom010RepositoriosJakartaData = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("DataRepository")
            .should().resideInAPackage(Raiz.pacote("command.persistence.repository"))
            .allowEmptyShould(true)
            .as("NOM-010: *DATAREPOSITORY FICA EM COMMAND.PERSISTENCE.REPOSITORY")
            .because("é o repositório Jakarta Data da escrita." + RENOMEIE);

    @ArchTest
    static final ArchRule nom011Excecoes = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Exception")
            .and().resideInAnyPackage(Raiz.pacote("command"), Raiz.pacote("query"))
            .should().resideInAPackage(Raiz.emQualquer("exception"))
            .allowEmptyShould(true)
            .as("NOM-011: *EXCEPTION DO NÚCLEO FICA NUM PACOTE EXCEPTION")
            .because("exceção é agrupada por tipo (command.domain.exception, command.application.exception)."
                    + " A de sistema externo fica na raiz de shared.gateway." + RENOMEIE);

    @ArchTest
    static final ArchRule nom012EnumsDoDominio = ArchRuleDefinition.classes()
            .that().areEnums().and().resideInAPackage(Raiz.pacote("command.domain"))
            .should().resideInAPackage(Raiz.pacote("command.domain.enums"))
            .allowEmptyShould(true)
            .as("NOM-012: ENUM DO DOMÍNIO FICA EM COMMAND.DOMAIN.ENUMS")
            .because("enum é agrupado por tipo." + " Corrija: mova o enum para command.domain.enums");
}
