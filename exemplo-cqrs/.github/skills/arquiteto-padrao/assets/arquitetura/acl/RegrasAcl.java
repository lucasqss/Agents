package PACOTE_RAIZ.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** ACL: o componente só traduz. Entrypoint chama use case; use case chama Gateway e Translator. */
final class RegrasAcl {

    private static final String TRANSLATOR = Raiz.pacote("translator");

    private RegrasAcl() {
    }

    @ArchTest
    static final ArchRule acl001SemBancoDeNegocio = ArchRuleDefinition.noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "jakarta.data..", "jakarta.transaction..", "org.hibernate..")
            .as("ACL-001: ACL NÃO TEM BANCO NEM TRANSAÇÃO")
            .because("o ACL não é dono de dado de negócio; quem guarda e decide é o componente negocial."
                    + " Corrija: mova a entidade e a regra para o negocial; estado técnico (cache, mensagem já processada)"
                    + " fica atrás de um *Gateway, com aprovação do dono");

    @ArchTest
    static final ArchRule acl002TranslatorPuro = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(TRANSLATOR)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..", "jakarta.inject..", "jakarta.ws.rs..", "jakarta.validation..",
                    "io.quarkus..", "org.eclipse.microprofile..")
            .as("ACL-002: TRANSLATOR NÃO PODE DEPENDER DE FRAMEWORK")
            .because("tradução é função pura, testada sem subir o Quarkus."
                    + " Corrija: tire @Inject/@ApplicationScoped; o *Translator é final, com métodos estáticos");

    @ArchTest
    static final ArchRule acl003TranslatorSoConheceDados = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(TRANSLATOR)
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("application"))
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Resource")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Client")
            .orShould().dependOnClassesThat().haveSimpleNameEndingWith("Gateway")
            .as("ACL-003: TRANSLATOR SÓ CONHECE OS DADOS DOS DOIS LADOS")
            .because("ele recebe um modelo e devolve o outro; não chama ninguém."
                    + " Corrija: o *UseCase chama o *Gateway e passa o resultado ao *Translator");

    @ArchTest
    static final ArchRule acl004SoUseCaseChamaTranslator = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages(TRANSLATOR, Raiz.pacote("application"))
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Translator")
            .as("ACL-004: SÓ USE CASE CHAMA TRANSLATOR")
            .because("o fluxo é entrypoint -> use case -> gateway e translator."
                    + " Corrija: chame o *Translator no *UseCase; entrypoint e gateway só veem os records que ele devolve");

    @ArchTest
    static final ArchRule acl005TranslatorNoLugar = ArchRuleDefinition.classes()
            .that().haveSimpleNameEndingWith("Translator")
            .should().resideInAPackage(Raiz.pacote("translator.*"))
            .as("ACL-005: *TRANSLATOR FICA EM TRANSLATOR.<SISTEMA>")
            .because("o sufixo diz a camada, e a tradução se organiza por sistema externo."
                    + " Corrija: mova a classe para translator/<sistema> ou troque o sufixo do nome");

    @ArchTest
    static final ArchRule acl006UseCaseDevolveONossoModelo = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().resideInAPackage(Raiz.pacote("application"))
            .and().arePublic()
            .should(Condicoes.naoRetornarModeloExterno())
            .as("ACL-006: USE CASE NÃO PODE DEVOLVER MODELO DE SISTEMA EXTERNO")
            .because("o que sai do ACL já está no nosso vocabulário."
                    + " Corrija: passe o retorno do *Gateway pelo *Translator e devolva o record de translator/<sistema>");
}
