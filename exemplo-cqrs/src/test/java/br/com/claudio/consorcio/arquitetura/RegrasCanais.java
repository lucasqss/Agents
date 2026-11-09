package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.data.repository.Repository;

/**
 * CAN: canais de atendimento (web, mobile, TAA...). Cada canal fica em entrypoint.&lt;canal&gt;.&lt;agregado&gt;, com o
 * padrão de apresentação na raiz do canal. Nenhuma regra lista os canais: o nome do subpacote é o canal, e
 * um canal novo herda tudo. CAN-002 e CAN-003 foram aposentadas: a CAN-001 passou a cobri-las.
 */
final class RegrasCanais {

    private static final String APRESENTACAO = Raiz.pacote("entrypoint.apresentacao");

    private RegrasCanais() {
    }

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can001CanaisNaoSeConhecem = SlicesRuleDefinition.slices()
            .matching(Raiz.pacote("entrypoint.(*)"))
            .should().notDependOnEachOther()
            .ignoreDependency(DescribedPredicate.alwaysTrue(), JavaClass.Predicates.resideInAPackage(APRESENTACAO))
            .allowEmptyShould(true)
            .as("CAN-001: UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL")
            .because("cada canal tem o próprio contrato e evolui sozinho."
                    + " Corrija: copie o Response/Assembler para o seu canal; o que for comum vai para o use case"
                    + " ou para entrypoint/apresentacao");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can004ApresentacaoSoNoEntrypoint = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("entrypoint"))
            .should().dependOnClassesThat().resideInAPackage(APRESENTACAO)
            .as("CAN-004: FORMATAÇÃO SÓ NO ENTRYPOINT")
            .because("use case e consulta não formatam nada; o mesmo dado atende todos os canais."
                    + " Corrija: devolva o valor cru e formate no Assembler/Resource do canal");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can005BaseComumSemCanal = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(APRESENTACAO)
            .should().dependOnClassesThat(Condicoes.estaoEmCanal())
            .as("CAN-005: BASE COMUM DE APRESENTAÇÃO NÃO PODE DEPENDER DE UM CANAL")
            .because("Formatador e Rotulos são a base que todos os canais usam."
                    + " Corrija: mova o que é específico para entrypoint/<canal>");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can006UseCasesSemCanal = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("application"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("entrypoint"))
            .as("CAN-006: USE CASE NÃO PODE DEPENDER DE ENTRYPOINT")
            .because("o mesmo use case atende todos os canais."
                    + " Corrija: receba e devolva tipos do application; o entrypoint converte Request/Response");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can007ContratoDoCanalIsolado = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat(Condicoes.estaoEmCanal())
            .and().arePublic()
            .should(Condicoes.naoRetornarTipoDePersistence())
            .allowEmptyShould(true)
            .as("CAN-007: ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE")
            .because("mudar uma projeção não pode mudar o JSON do front."
                    + " Corrija: devolva o *Response do canal, montado pelo Assembler");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can008ApresentacaoSemFramework = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("Assembler")
            .or().haveSimpleNameStartingWith("Padrao")
            .or().resideInAPackage(APRESENTACAO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..", "jakarta.inject..", "jakarta.persistence..", "io.quarkus..")
            .as("CAN-008: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK")
            .because("são funções puras, testáveis sem Quarkus."
                    + " Corrija: tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA");

    @ArchIgnore(reason = "adequacao:entrypoint")
    @ArchTest
    static final ArchRule can009ApresentacaoSemRepositorio = ArchRuleDefinition.noClasses()
            .that().haveSimpleNameEndingWith("Assembler")
            .or().haveSimpleNameStartingWith("Padrao")
            .or().resideInAPackage(APRESENTACAO)
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("CAN-009: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO")
            .because("montar a resposta não busca dados."
                    + " Corrija: o use case busca tudo que a tela precisa; o Assembler só transforma");
}
