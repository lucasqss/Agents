package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** QRY: leitura de tela. entrypoint -> application -> *QueryRepository, sem passar pelo domínio. */
final class RegrasLeitura {

    private RegrasLeitura() {
    }

    @ArchTest
    static final ArchRule qry001ConsultaIndependenteDoDominio = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("QueryRepository")
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("domain"))
            .as("QRY-001: *QUERYREPOSITORY NÃO PODE DEPENDER DO DOMÍNIO")
            .because("consulta de tela não passa pelo domínio."
                    + " Corrija: crie um record de projeção e a consulta em persistence/<agregado>; não reuse o domínio");

    @ArchTest
    static final ArchRule qry002PersistenciaNaBase = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("persistence"))
            .should().dependOnClassesThat().resideInAnyPackage(Raiz.pacote("application"), Raiz.pacote("entrypoint"))
            .as("QRY-002: PERSISTENCE NÃO PODE DEPENDER DE APPLICATION NEM ENTRYPOINT")
            .because("a direção é entrypoint -> application -> persistence."
                    + " Corrija: o repositório devolve records próprios; quem adapta é o use case ou o entrypoint");

    @ArchTest
    static final ArchRule qry003EntrypointSemRepositorio = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("entrypoint"))
            .should().dependOnClassesThat().areAnnotatedWith(Condicoes.REPOSITORY)
            .as("QRY-003: ENTRYPOINT NÃO PODE CHAMAR REPOSITÓRIO")
            .because("o entrypoint chama use cases; dos repositórios ele só vê os records projetados."
                    + " Corrija: crie ou use um *UseCase em application/<agregado>");
}
