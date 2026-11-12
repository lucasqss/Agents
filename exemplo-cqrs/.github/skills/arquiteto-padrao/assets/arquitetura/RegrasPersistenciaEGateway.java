package PACOTE_RAIZ.arquitetura;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/** SHR (prefixo histórico; IDs não mudam): entidades do nosso banco e gateways de sistemas externos. */
final class RegrasPersistenciaEGateway {

    private static final String GATEWAY = Raiz.pacote("gateway");

    private RegrasPersistenciaEGateway() {
    }

    @ArchTest
    static final ArchRule shr001GatewayNaoConheceONegocio = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(GATEWAY).and().haveSimpleNameNotEndingWith("GatewayAdapter")
            .should().dependOnClassesThat().resideInAnyPackage(
                    Raiz.pacote("application"), Raiz.pacote("domain"), Raiz.pacote("entrypoint"), Raiz.pacote("persistence"))
            .as("SHR-001: GATEWAY NÃO PODE DEPENDER DAS OUTRAS CAMADAS")
            .because("o gateway só conhece o sistema externo; a exceção é o *GatewayAdapter, que implementa interface do domínio."
                    + " Corrija: use tipos próprios do gateway e converta no use case");

    @ArchTest
    static final ArchRule shr002EntidadesSemComportamento = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat().haveSimpleNameEndingWith("Entity")
            .and().doNotHaveModifier(JavaModifier.SYNTHETIC)
            .should().haveNameMatching("(get|set|is)[A-Z].*")
            .as("SHR-002: ENTIDADE JPA NÃO TEM REGRA DE NEGÓCIO")
            .because("entidade é só o formato da tabela."
                    + " Corrija: mova o método para o objeto do domínio em domain/<agregado>; na entidade, só get/set/is");

    @ArchTest
    static final ArchRule shr003ClientEDtoExternoNaoSaemDoGateway = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackage(GATEWAY)
            .should().dependOnClassesThat(Condicoes.internoDoGateway())
            .as("SHR-003: CLIENT E MODELO DE SISTEMA EXTERNO NÃO SAEM DO GATEWAY")
            .because("se o sistema externo mudar o contrato, só o gateway muda."
                    + " Corrija: fora de gateway/<sistema> use o *Gateway, as *Exception e os records próprios do gateway;"
                    + " record próprio não aparece no *Client nem usa classe de dependência de outro sistema");

    @ArchTest
    static final ArchRule shr004GatewaySoNosUseCases = ArchRuleDefinition.noClasses()
            .that().resideOutsideOfPackages(GATEWAY, Raiz.pacote("application"), Raiz.pacote("entrypoint"))
            .should().dependOnClassesThat().resideInAPackage(GATEWAY)
            .as("SHR-004: SÓ USE CASE CHAMA GATEWAY DE SISTEMA EXTERNO")
            .because("chamada externa é orquestração, papel do Service Layer; o entrypoint só traduz a exceção para HTTP."
                    + " Corrija: injete o *Gateway no *UseCase e passe o resultado adiante");

    @ArchTest
    static final ArchRule shr005SistemasExternosIndependentes = SlicesRuleDefinition.slices()
            .matching(Raiz.pacote("gateway.(*)"))
            .should().notDependOnEachOther()
            .allowEmptyShould(true)
            .as("SHR-005: UM GATEWAY NÃO PODE DEPENDER DE OUTRO")
            .because("cada sistema externo tem o próprio gateway."
                    + " Corrija: quem combina dois sistemas é o use case");

    @ArchTest
    static final ArchRule shr006EntrypointNaoChamaGateway = ArchRuleDefinition.noClasses()
            .that().resideInAPackage(Raiz.pacote("entrypoint"))
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Gateway")
            .as("SHR-006: ENTRYPOINT NÃO PODE CHAMAR GATEWAY")
            .because("o entrypoint só traduz HTTP e, do gateway, só conhece a exceção."
                    + " Corrija: crie ou use um *UseCase que chama o *Gateway");
}
