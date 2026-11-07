package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.naoRetornarTipoDePersistence;
import static br.com.claudio.consorcio.arquitetura.Condicoes.usadoSoPeloProprioCanal;
import static br.com.claudio.consorcio.arquitetura.Raiz.emQualquer;
import static br.com.claudio.consorcio.arquitetura.Raiz.exato;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.data.repository.Repository;

/**
 * CAN: canais de atendimento (web, mobile, TAA...). Um BFF por canal em entrypoint.&lt;canal&gt; e um padrão de
 * apresentação por canal em shared.apresentacao.&lt;canal&gt;. Nenhuma regra lista os canais: o nome do
 * subpacote é o canal, e um canal novo herda tudo.
 */
final class RegrasCanais {

    private RegrasCanais() {
    }

    @ArchTest
    static final ArchRule can001CanaisNaoSeConhecem = slices()
            .matching(emQualquer("entrypoint.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .as("CAN-001: UM CANAL NÃO PODE USAR CLASSE DE OUTRO CANAL")
            .because("cada canal tem o próprio contrato e evolui sozinho."
                    + " Corrija: copie o Response/Assembler para o seu canal; o que for comum vai para o use case");

    @ArchTest
    static final ArchRule can002PadroesNaoSeConhecem = slices()
            .matching(pacote("shared.apresentacao.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .as("CAN-002: PADRÃO DE APRESENTAÇÃO DE UM CANAL NÃO PODE USAR O DE OUTRO")
            .because("cada canal segue o padrão da própria plataforma."
                    + " Corrija: o que é comum aos canais vai para a base shared.apresentacao (Formatador, Rotulos)");

    @ArchTest
    static final ArchRule can003PadraoDoCanalSoNoProprioCanal = classes()
            .that().resideInAPackage(pacote("shared.apresentacao.*"))
            .should(usadoSoPeloProprioCanal())
            .allowEmptyShould(true)
            .as("CAN-003: PADRÃO DO CANAL SÓ É USADO PELO ENTRYPOINT DO MESMO CANAL")
            .because("o mobile não pode sair formatado no padrão web."
                    + " Corrija: em entrypoint.<canal> use só shared.apresentacao.<canal> (ou a base comum)");

    @ArchTest
    static final ArchRule can004ApresentacaoSoNoEntrypoint = noClasses()
            .that().resideOutsideOfPackages(emQualquer("entrypoint"), pacote("shared.apresentacao"))
            .should().dependOnClassesThat().resideInAPackage(pacote("shared.apresentacao"))
            .as("CAN-004: FORMATAÇÃO SÓ NO ENTRYPOINT")
            .because("use case e consulta não formatam nada; o mesmo dado atende todos os canais."
                    + " Corrija: devolva o valor cru e formate no Assembler/Resource do canal");

    @ArchTest
    static final ArchRule can005BaseComumSemCanal = noClasses()
            .that().resideInAPackage(exato("shared.apresentacao"))
            .should().dependOnClassesThat().resideInAPackage(pacote("shared.apresentacao.*"))
            .as("CAN-005: BASE COMUM DE APRESENTAÇÃO NÃO PODE DEPENDER DE UM CANAL")
            .because("Formatador e Rotulos são a base que todos os canais usam."
                    + " Corrija: mova o que é específico para shared.apresentacao.<canal>");

    @ArchTest
    static final ArchRule can006UseCasesSemCanal = noClasses()
            .that().resideInAPackage(emQualquer("application"))
            .should().dependOnClassesThat().resideInAPackage(emQualquer("entrypoint"))
            .as("CAN-006: USE CASE NÃO PODE DEPENDER DE ENTRYPOINT")
            .because("o mesmo use case atende todos os canais."
                    + " Corrija: receba e devolva tipos do application; o entrypoint converte Request/Response");

    @ArchTest
    static final ArchRule can007ContratoDoCanalIsolado = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(emQualquer("entrypoint.*"))
            .and().arePublic()
            .should(naoRetornarTipoDePersistence())
            .allowEmptyShould(true)
            .as("CAN-007: ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE PERSISTENCE")
            .because("mudar uma projeção não pode mudar o JSON do front."
                    + " Corrija: devolva o *Response do canal, montado pelo Assembler");

    @ArchTest
    static final ArchRule can008ApresentacaoSemFramework = noClasses()
            .that().haveSimpleNameEndingWith("Assembler").or().resideInAPackage(pacote("shared.apresentacao"))
            .should().dependOnClassesThat().resideInAnyPackage(
                    "jakarta.enterprise..", "jakarta.inject..", "jakarta.persistence..", "io.quarkus..")
            .as("CAN-008: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM DEPENDER DE FRAMEWORK")
            .because("são funções puras, testáveis sem Quarkus."
                    + " Corrija: tire @Inject/@ApplicationScoped; use métodos estáticos ou a instância INSTANCIA");

    @ArchTest
    static final ArchRule can009ApresentacaoSemRepositorio = noClasses()
            .that().haveSimpleNameEndingWith("Assembler").or().resideInAPackage(pacote("shared.apresentacao"))
            .should().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .as("CAN-009: ASSEMBLER E PADRÃO DE CANAL NÃO PODEM CONSULTAR O BANCO")
            .because("montar a resposta não busca dados."
                    + " Corrija: o use case busca tudo que a tela precisa; o Assembler só transforma");
}
