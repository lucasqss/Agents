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
}
