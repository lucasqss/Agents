package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/**
 * QRY: lado de leitura. application -> persistence, sem domínio e sem portas; o HTTP fica nos canais.
 * <p>ID vago: QRY-003 (entrypoint de leitura sem repositório), hoje CAN-011.
 */
final class RegrasQuery {

    private RegrasQuery() {
    }

    @ArchTest
    static final ArchRule qry001ConsultaIndependenteDoComando = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("query"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command"))
            .as("QRY-001: QUERY NÃO PODE DEPENDER DE COMMAND")
            .because("leitura e escrita evoluem separadas (CQRS)."
                    + " Corrija: crie um record de projeção e a consulta em query.persistence; não reuse o domínio");

    @ArchTest
    static final ArchRule qry002PersistenciaNaBase = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("query.persistence"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("query.application"))
            .as("QRY-002: QUERY.PERSISTENCE NÃO PODE DEPENDER DE QUERY.APPLICATION")
            .because("a direção é canal -> application -> persistence."
                    + " Corrija: o repositório devolve records próprios; quem adapta é o use case ou o canal");

    @ArchTest
    static final ArchRule qry004ApplicationAgrupadoPorTipo = ArchRuleDefinition.classes()
            .that().resideInAPackage(Raiz.pacote("query.application"))
            .and().doNotHaveSimpleName("package-info")
            .should().resideInAnyPackage(Raiz.pacote("query.application.usecase"), Raiz.pacote("query.application.dto"))
            .as("QRY-004: QUERY.APPLICATION AGRUPADO POR TIPO (USECASE, DTO)")
            .because("cada tipo de classe tem lugar fixo."
                    + " Corrija: *UseCase em usecase, tipos de saída comuns (ex.: Pagina) em dto");

    @ArchTest
    static final ArchRule qry005PersistenceAgrupadoPorTipo = ArchRuleDefinition.classes()
            .that().resideInAPackage(Raiz.pacote("query.persistence"))
            .and().doNotHaveSimpleName("package-info")
            .should().resideInAnyPackage(
                    Raiz.pacote("query.persistence.repository"), Raiz.pacote("query.persistence.projection"))
            .as("QRY-005: QUERY.PERSISTENCE AGRUPADO POR TIPO (REPOSITORY, PROJECTION)")
            .because("cada tipo de classe tem lugar fixo."
                    + " Corrija: *QueryRepository em repository, record projetado em projection");
}
