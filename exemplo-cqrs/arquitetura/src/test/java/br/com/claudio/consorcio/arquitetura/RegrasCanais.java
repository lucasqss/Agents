package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.data.repository.Repository;

/**
 * CAN: canais de atendimento (web, mobile, TAA...). Cada canal é um módulo/aplicação próprio, com o pacote
 * canal.&lt;canal&gt;; a base de todos fica em canal.comum. Nenhuma regra lista os canais: o nome do subpacote é o
 * canal, e um canal novo herda tudo.
 *
 * <p>IDs vagos (regras que deixaram de fazer sentido com um módulo por canal): CAN-002, CAN-003, CAN-004 e
 * CAN-006, hoje cobertos por CAN-001 e GER-003.
 */
final class RegrasCanais {

    private RegrasCanais() {
    }

    @ArchTest
    static final ArchRule can001CanaisNaoSeConhecem = SlicesRuleDefinition.slices()
            .matching(Raiz.PACOTE + ".canal.(*)..")
            .should().notDependOnEachOther()
            .ignoreDependency(DescribedPredicate.<JavaClass>alwaysTrue(),
                    JavaClass.Predicates.resideInAPackage(Raiz.pacote("canal.comum")))
            .allowEmptyShould(true)
            .as("CAN-001: UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL")
            .because("cada canal tem o próprio contrato, padrão e dependências corporativas, e sobe sozinho."
                    + " Corrija: copie o que precisa para o seu canal; o que é comum a todos vai para canal.comum");

    @ArchTest
    static final ArchRule can005BaseComumSemCanal = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("canal.comum"))
            .should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage(Raiz.pacote("canal"))
                    .and(JavaClass.Predicates.resideOutsideOfPackage(Raiz.pacote("canal.comum"))))
            .as("CAN-005: CANAL.COMUM NÃO PODE DEPENDER DE UM CANAL")
            .because("canal.comum é a base que todos os canais usam."
                    + " Corrija: mova o que é específico para o módulo do canal");

    @ArchTest
    static final ArchRule can007ContratoDoCanalIsolado = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().resideInAPackage(Raiz.pacote("canal"))
            .and().arePublic()
            .should(Condicoes.naoRetornarTipoDePersistence())
            .allowEmptyShould(true)
            .as("CAN-007: ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE")
            .because("mudar uma projeção não pode mudar o JSON do front."
                    + " Corrija: devolva o *Response do canal, montado pelo Assembler");

    @ArchTest
    static final ArchRule can008ApresentacaoSemFramework = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("Assembler").or().resideInAPackage(Raiz.emQualquer("apresentacao"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..", "jakarta.inject..", "jakarta.persistence..", "io.quarkus..")
            .as("CAN-008: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK")
            .because("são funções puras, testáveis sem Quarkus."
                    + " Corrija: tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA");

    @ArchTest
    static final ArchRule can009ApresentacaoSemRepositorio = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("Assembler").or().resideInAPackage(Raiz.emQualquer("apresentacao"))
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("CAN-009: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO")
            .because("montar a resposta não busca dados."
                    + " Corrija: o use case busca tudo que a tela precisa; o Assembler só transforma");

    @ArchTest
    static final ArchRule can010CanalSomenteLeituraNaoEscreve = ArchRuleDefinition.noClasses()
            .that(Condicoes.deCanalSomenteLeitura())
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("command"))
            .allowEmptyShould(true)
            .as("CAN-010: CANAL @SOMENTELEITURA NÃO PODE USAR O LADO DE ESCRITA")
            .because("a escrita está bloqueada neste canal (package-info do canal)."
                    + " Corrija: use só query.application; se o canal passou a escrever, isso é decisão de negócio:"
                    + " tire o @SomenteLeitura no PR e libere o tipo em CanalDeAtendimento");

    @ArchTest
    static final ArchRule can011CanalSemBanco = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("canal"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    Raiz.pacote("command.persistence"), Raiz.pacote("shared.persistence"),
                    "jakarta.persistence..", "jakarta.data..")
            .orShould().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("CAN-011: CANAL NÃO PODE ACESSAR O BANCO")
            .because("o canal só traduz HTTP e chama use case; dos repositórios ele só vê os records projetados."
                    + " Corrija: crie ou use um *UseCase em command.application ou query.application");
}
