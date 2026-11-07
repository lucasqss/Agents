package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/** SHR: o que é compartilhado. Entidades do nosso banco, gateways de sistemas externos e apresentação. */
final class RegrasShared {

    private RegrasShared() {
    }

    @ArchTest
    static final ArchRule shr001SharedNaoConheceOsLados = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("shared"))
            .should().dependOnClassesThat().resideInAnyPackage(Raiz.pacote("command"), Raiz.pacote("query"))
            .as("SHR-001: SHARED NÃO PODE DEPENDER DE COMMAND NEM QUERY")
            .because("shared é usado pelos dois lados, nunca o contrário; por isso um gateway nunca importa o domínio."
                    + " Corrija: use tipos próprios do shared (ex.: DTO do gateway) e converta no use case");

    @ArchTest
    static final ArchRule shr002EntidadesSemComportamento = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().resideInAPackage(Raiz.pacote("shared.persistence"))
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveNameMatching("(get|set|is)[A-Z].*")
            .as("SHR-002: ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO")
            .because("entidade é só o formato da tabela."
                    + " Corrija: mova o método para o objeto do domínio em command.domain; na entidade, só get/set/is");

    @ArchTest
    static final ArchRule shr003ClientEDtoExternoNaoSaemDoGateway = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(Raiz.pacote("shared.gateway"))
            .should().dependOnClassesThat(Condicoes.internoDoGateway())
            .as("SHR-003: CLIENT E DTO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY")
            .because("se o sistema externo mudar o contrato, só o gateway muda."
                    + " Corrija: fora de shared.gateway use só a classe *Gateway e as *Exception dele");

    @ArchTest
    static final ArchRule shr004GatewaySoNosUseCases = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages(
                    Raiz.pacote("shared.gateway"), Raiz.emQualquer("application"), Raiz.emQualquer("entrypoint"), Raiz.pacote("command.gateway"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("shared.gateway"))
            .as("SHR-004: SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO")
            .because("chamada externa é orquestração, papel do Service Layer; o entrypoint só traduz a exceção para HTTP."
                    + " Corrija: injete o *Gateway no *UseCase e passe o resultado adiante");

    @ArchTest
    static final ArchRule shr005SistemasExternosIndependentes = SlicesRuleDefinition.slices()
            .matching(Raiz.pacote("shared.gateway.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .as("SHR-005: UM GATEWAY NÃO PODE DEPENDER DE OUTRO")
            .because("cada sistema externo tem o próprio gateway."
                    + " Corrija: quem combina dois sistemas é o use case");
}
