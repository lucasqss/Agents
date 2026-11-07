package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import jakarta.data.repository.Repository;

/** QRY: lado de leitura. entrypoint -> application -> persistence, sem domínio e sem portas. */
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
            .should().dependOnClassesThat().resideInAnyPackage(Raiz.pacote("query.application"), Raiz.pacote("query.entrypoint"))
            .as("QRY-002: QUERY.PERSISTENCE NÃO PODE DEPENDER DE APPLICATION NEM ENTRYPOINT")
            .because("a direção é entrypoint -> application -> persistence."
                    + " Corrija: o repositório devolve records próprios; quem adapta é o use case ou o entrypoint");

    @ArchTest
    static final ArchRule qry003EntrypointSemRepositorio = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("query.entrypoint"))
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("QRY-003: ENTRYPOINT DE LEITURA NÃO PODE CHAMAR REPOSITÓRIO")
            .because("o entrypoint chama use cases; dos repositórios ele só vê os records projetados."
                    + " Corrija: crie ou use um *UseCase em query.application");
}
