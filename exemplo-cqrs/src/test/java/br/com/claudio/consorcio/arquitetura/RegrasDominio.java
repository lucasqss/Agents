package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** DOM: Domain Model puro (PoEAA). Java sem framework e sem conhecer as outras camadas. */
final class RegrasDominio {

    private static final String DOMINIO = Raiz.pacote("command.domain");

    private RegrasDominio() {
    }

    @ArchTest
    static final ArchRule dom001Framework = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..",
                    "jakarta.inject..",
                    "jakarta.transaction..",
                    "io.quarkus..",
                    "org.eclipse.microprofile..")
            .as("DOM-001: DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK")
            .because("o domínio é Java puro, testável sem subir o Quarkus."
                    + " Corrija: tire @Inject/@ApplicationScoped/@Transactional do domínio;"
                    + " quem injeta e abre transação é o use case (command.application)");

    @ArchTest
    static final ArchRule dom002Persistencia = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.persistence..",
                    "jakarta.data..",
                    "org.hibernate..",
                    Raiz.pacote("command.persistence"),
                    Raiz.pacote("shared.persistence"))
            .as("DOM-002: DOMÍNIO NÃO PODE DEPENDER DE PERSISTÊNCIA")
            .because("o domínio não sabe que existe banco."
                    + " Corrija: declare uma interface de repositório no domínio; a implementação,"
                    + " a entidade JPA e o Mapper ficam em command.persistence e shared.persistence");

    @ArchTest
    static final ArchRule dom003Application = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command.application"))
            .as("DOM-003: DOMÍNIO NÃO PODE DEPENDER DE APPLICATION (SERVICE LAYER)")
            .because("o use case usa o domínio, nunca o contrário."
                    + " Corrija: passe o dado como parâmetro do método do domínio ou mova a lógica para o use case");

    @ArchTest
    static final ArchRule dom004Gateway = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage(Raiz.pacote("shared.gateway"), Raiz.pacote("command.gateway"))
            .as("DOM-004: DOMÍNIO NÃO PODE DEPENDER DE GATEWAY EXTERNO")
            .because("só use cases chamam sistemas externos."
                    + " Corrija: o use case chama o Gateway e passa o resultado para o domínio;"
                    + " se a regra do domínio precisa do sistema, declare a interface no domínio e implemente em command.gateway");

    @ArchTest
    static final ArchRule dom005Http = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.ws.rs..", Raiz.pacote("command.entrypoint"))
            .as("DOM-005: DOMÍNIO NÃO PODE DEPENDER DE HTTP NEM DO ENTRYPOINT")
            .because("HTTP é detalhe de entrada."
                    + " Corrija: lance uma DominioException; o entrypoint traduz para o status HTTP no ExceptionMappers");

    @ArchTest
    static final ArchRule dom006BeanValidation = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAPackage("jakarta.validation..")
            .as("DOM-006: DOMÍNIO NÃO PODE USAR BEAN VALIDATION")
            .because("o domínio protege as próprias invariantes."
                    + " Corrija: valide formato (@NotBlank, @Size) no Request do entrypoint e regra de negócio"
                    + " no construtor/método do domínio lançando DominioException");

    @ArchTest
    static final ArchRule dom007Apresentacao = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(DOMINIO)
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("shared.apresentacao"))
            .as("DOM-007: DOMÍNIO NÃO PODE DEPENDER DE APRESENTAÇÃO DE CANAL")
            .because("formatação muda por canal; regra de negócio não."
                    + " Corrija: devolva o valor cru e formate no entrypoint do canal com o Padrao do canal");
}
