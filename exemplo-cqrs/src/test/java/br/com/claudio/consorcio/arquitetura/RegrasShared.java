package br.com.claudio.consorcio.arquitetura;

import static br.com.claudio.consorcio.arquitetura.Condicoes.internoDoGateway;
import static br.com.claudio.consorcio.arquitetura.Raiz.emQualquer;
import static br.com.claudio.consorcio.arquitetura.Raiz.pacote;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** SHR: o que é compartilhado. Entidades do nosso banco, gateways de sistemas externos e apresentação. */
final class RegrasShared {

    private RegrasShared() {
    }

    @ArchTest
    static final ArchRule shr001SharedNaoConheceOsLados = noClasses()
            .that().resideInAPackage(pacote("shared"))
            .should().dependOnClassesThat().resideInAnyPackage(pacote("command"), pacote("query"))
            .as("SHR-001: SHARED NÃO PODE DEPENDER DE COMMAND NEM QUERY")
            .because("shared é usado pelos dois lados, nunca o contrário; por isso um gateway nunca importa o domínio."
                    + " Corrija: use tipos próprios do shared (ex.: DTO do gateway) e converta no use case");

    @ArchTest
    static final ArchRule shr002EntidadesSemComportamento = methods()
            .that().areDeclaredInClassesThat().resideInAPackage(pacote("shared.persistence"))
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveNameMatching("(get|set|is)[A-Z].*")
            .as("SHR-002: ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO")
            .because("entidade é só o formato da tabela."
                    + " Corrija: mova o método para o objeto do domínio em command.domain; na entidade, só get/set/is");

    @ArchTest
    static final ArchRule shr003ClientEDtoExternoNaoSaemDoGateway = noClasses()
            .that().resideOutsideOfPackage(pacote("shared.gateway"))
            .should().dependOnClassesThat(internoDoGateway())
            .as("SHR-003: CLIENT E DTO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY")
            .because("se o sistema externo mudar o contrato, só o gateway muda."
                    + " Corrija: fora de shared.gateway use só a classe *Gateway e as *Exception dele");

    @ArchTest
    static final ArchRule shr004GatewaySoNosUseCases = noClasses()
            .that().resideOutsideOfPackages(
                    pacote("shared.gateway"), emQualquer("application"), emQualquer("entrypoint"), pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(pacote("shared.gateway"))
            .as("SHR-004: SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO")
            .because("chamada externa é orquestração, papel do Service Layer; o entrypoint só traduz a exceção para HTTP."
                    + " Corrija: injete o *Gateway no *UseCase e passe o resultado adiante");

    @ArchTest
    static final ArchRule shr005SistemasExternosIndependentes = slices()
            .matching(pacote("shared.gateway.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .as("SHR-005: UM GATEWAY NÃO PODE DEPENDER DE OUTRO")
            .because("cada sistema externo tem o próprio gateway."
                    + " Corrija: quem combina dois sistemas é o use case");
}
