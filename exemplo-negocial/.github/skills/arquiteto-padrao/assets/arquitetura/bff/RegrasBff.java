package PACOTE_RAIZ.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;

/** BFF: o componente só adapta os negociais para um canal. Entrypoint chama use case; use case chama Gateway. */
final class RegrasBff {

    private RegrasBff() {
    }

    @ArchTest
    static final ArchRule bff001SemBancoDeNegocio = ArchRuleDefinition.noClasses()
            .should().dependOnClassesThat().resideInAnyPackage("jakarta.persistence..", "jakarta.data..", "jakarta.transaction..", "org.hibernate..")
            .as("BFF-001: BFF NÃO TEM BANCO NEM TRANSAÇÃO")
            .because("o BFF não é dono de dado; uma escrita vai para um único componente negocial, que abre a transação."
                    + " Corrija: mova a entidade e a regra para o negocial e chame-o por um *Gateway");

    @ArchTest
    static final ArchRule bff002UmCanalSo = ArchRuleDefinition.classes()
            .that(Condicoes.estaoEmCanal())
            .should(Condicoes.estarNoUnicoCanal())
            .as("BFF-002: UM BFF ATENDE UM CANAL SÓ")
            .because("o BFF existe para o canal evoluir no próprio ritmo; dois canais no mesmo BFF voltam a se travar."
                    + " Corrija: crie outro componente BFF para o outro canal");

    @ArchTest
    static final ArchRule bff003ContratoDoCanalIsolado = ArchRuleDefinition.methods()
            .that().areDeclaredInClassesThat(Condicoes.estaoEmCanal())
            .and().arePublic()
            .should(Condicoes.naoRetornarTipoDe("gateway"))
            .as("BFF-003: ENDPOINT DE CANAL NÃO PODE DEVOLVER TIPO DE GATEWAY")
            .because("mudar a API do negocial não pode mudar o JSON do front."
                    + " Corrija: devolva o *Response do canal, montado pelo Assembler");

    @ArchTest
    static final ArchRule bff004SoCanalNoEntrypoint = ArchRuleDefinition.noClasses()
            .should().resideInAnyPackage(
                    Raiz.pacote("entrypoint.rest"), Raiz.pacote("entrypoint.mensageria"), Raiz.pacote("entrypoint.batch"))
            .as("BFF-004: BFF SÓ TEM ENTRADA DE CANAL")
            .because("quem chama o BFF é o front; API neutra, fila e job pertencem ao componente negocial."
                    + " Corrija: mova o endpoint para entrypoint/<canal>/<recurso> ou para o negocial");
}
