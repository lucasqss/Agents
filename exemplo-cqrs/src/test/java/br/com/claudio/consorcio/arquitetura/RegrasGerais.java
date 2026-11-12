package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import jakarta.ws.rs.Path;

/** GER: estrutura geral do projeto. Camadas no topo, agregados dentro delas. */
final class RegrasGerais {

    private static final String TIPOS_DA_RAIZ =
            ".*\\.(ExceptionMappers|ErroResponse|IdCriado|Pagina|package-info|[A-Za-z]*Exception)(\\$.*)?";

    private RegrasGerais() {
    }

    /**
     * Esta regra NUNCA passa vazia: se a pasta foi copiada para o pacote errado (e nenhuma classe foi
     * encontrada), ela falha em vez de deixar tudo passar. As camadas vêm do {@link Tipo} do componente.
     */
    @ArchTest
    static final ArchRule ger001Estrutura = ArchRuleDefinition.classes()
            .should().resideInAnyPackage(Tipo.CAMADAS.stream().map(Raiz::pacote).toArray(String[]::new))
            .allowEmptyShould(false)
            .as("GER-001: TODO CÓDIGO FICA EM UMA DAS CAMADAS DO TIPO DO COMPONENTE")
            .because("um componente " + Tipo.NOME + " só tem " + String.join(", ", Tipo.CAMADAS) + "."
                    + " Corrija: mova a classe para a camada certa; se a camada não existe neste tipo, o código pertence"
                    + " a outro componente; se nenhuma classe foi analisada, a pasta arquitetura está no pacote errado"
                    + " (deve ser <raiz>.arquitetura)");

    @ArchTest
    static final ArchRule ger002SemCiclos = SlicesRuleDefinition.slices()
            .matching(Raiz.PACOTE + ".(*).(*)..")
            .should().beFreeOfCycles()
            .as("GER-002: SEM CICLOS ENTRE PACOTES")
            .because("ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra."
                    + " Corrija: inverta a dependência com uma interface ou mova a classe compartilhada");

    @ArchTest
    static final ArchRule ger003RaizDaCamada = ArchRuleDefinition.classes()
            .that().resideInAnyPackage(
                    Raiz.exato("entrypoint"),
                    Raiz.exato("application"),
                    Raiz.exato("domain"),
                    Raiz.exato("persistence"),
                    Raiz.exato("gateway"),
                    Raiz.exato("translator"))
            .should().haveNameMatching(TIPOS_DA_RAIZ)
            .as("GER-003: RAIZ DA CAMADA SÓ ACEITA OS TIPOS LISTADOS")
            .because("sem limite, a raiz da camada vira um novo shared."
                    + " Corrija: mova a classe para <camada>/<agregado>; a raiz só aceita exceções comuns,"
                    + " ExceptionMappers, ErroResponse, IdCriado, Pagina e package-info");

    @ArchTest
    static final ArchRule ger004SemPastasProibidas = ArchRuleDefinition.noClasses()
            .should().resideInAnyPackage(
                    Raiz.emQualquer("shared"),
                    Raiz.emQualquer("common"),
                    Raiz.emQualquer("util"),
                    Raiz.emQualquer("command"),
                    Raiz.emQualquer("query"))
            .as("GER-004: NÃO EXISTEM PASTAS SHARED, COMMON, UTIL, COMMAND NEM QUERY")
            .because("essas pastas viram depósito ou separam por técnica o que é do mesmo agregado."
                    + " Corrija: mova a classe para <camada>/<agregado>");

    @ArchTest
    static final ArchRule ger005EndpointNaViaEAgregado = ArchRuleDefinition.classes()
            .that().areAnnotatedWith(Path.class).and().areNotAnnotatedWith(Condicoes.REST_CLIENT)
            .should().resideInAPackage(Raiz.exato("entrypoint.*.*"))
            .as("GER-005: ENDPOINT FICA EM ENTRYPOINT.<VIA>.<AGREGADO>")
            .because("primeiro a via (rest, web, mobile, taa), depois o agregado, sempre nessa ordem."
                    + " Corrija: mova o *Resource; se ele atende vários agregados, faça um Resource por agregado");
}
