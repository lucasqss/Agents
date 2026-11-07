package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.data.repository.Repository;

/** QRY: lado de leitura. entrypoint -> application -> persistence, sem domínio e sem portas. */
final class RegrasQuery {

    private RegrasQuery() {
    }

    @ArchTest
    static final ArchRule qry001ConsultaIndependenteDoComando = noClasses()
            .that().resideInAPackage(pacote("query"))
            .should().dependOnClassesThat().resideInAPackage(pacote("command"))
            .as("QRY-001: QUERY NÃO PODE DEPENDER DE COMMAND")
            .because("leitura e escrita evoluem separadas (CQRS)."
                    + " Corrija: crie um record de projeção e a consulta em query.persistence; não reuse o domínio");

    @ArchTest
    static final ArchRule qry002PersistenciaNaBase = noClasses()
            .that().resideInAPackage(pacote("query.persistence"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("query.application"), pacote("query.entrypoint"))
            .as("QRY-002: QUERY.PERSISTENCE NÃO PODE DEPENDER DE APPLICATION NEM ENTRYPOINT")
            .because("a direção é entrypoint -> application -> persistence."
                    + " Corrija: o repositório devolve records próprios; quem adapta é o use case ou o entrypoint");

    @ArchTest
    static final ArchRule qry003EntrypointSemRepositorio = noClasses()
            .that().resideInAPackage(pacote("query.entrypoint"))
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("QRY-003: ENTRYPOINT DE LEITURA NÃO PODE CHAMAR REPOSITÓRIO")
            .because("o entrypoint chama use cases; dos repositórios ele só vê os records projetados."
                    + " Corrija: crie ou use um *UseCase em query.application");
}
