package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;

/** GER: estrutura geral do projeto (núcleo em command/query/shared, canais em canal.&lt;canal&gt;). */
final class RegrasGerais {

    private RegrasGerais() {
    }

    /**
     * Esta regra NUNCA passa vazia: se a pasta foi copiada para o pacote errado (e nenhuma classe foi
     * encontrada), ela falha em vez de deixar tudo passar.
     */
    @ArchTest
    static final ArchRule ger001Estrutura = ArchRuleDefinition.classes()
            .should().resideInAnyPackage(
                    Raiz.pacote("command"), Raiz.pacote("query"), Raiz.pacote("shared"), Raiz.pacote("canal"))
            .allowEmptyShould(false)
            .as("GER-001: TODO CÓDIGO FICA EM COMMAND, QUERY, SHARED OU CANAL")
            .because("o núcleo tem command (escrita), query (leitura) e shared; cada canal fica em canal.<canal>."
                    + " Corrija: mova a classe para o lado certo; se nenhuma classe foi analisada,"
                    + " a pasta arquitetura está no pacote errado (deve ser <raiz>.arquitetura)");

    @ArchTest
    static final ArchRule ger002SemCiclos = SlicesRuleDefinition.slices()
            .matching(Raiz.PACOTE + ".(*).(*)..")
            .should().beFreeOfCycles()
            .as("GER-002: SEM CICLOS ENTRE PACOTES")
            .because("ciclo (A usa B e B usa A) impede mudar uma parte sem mexer na outra."
                    + " Corrija: inverta a dependência com uma interface ou mova a classe compartilhada");

    @ArchTest
    static final ArchRule ger003NucleoNaoConheceCanais = ArchRuleDefinition.noClasses()
            .that().resideInAnyPackage(Raiz.pacote("command"), Raiz.pacote("query"), Raiz.pacote("shared"))
            .should().dependOnClassesThat().resideInAPackage(Raiz.pacote("canal"))
            .as("GER-003: NÚCLEO NÃO PODE DEPENDER DE CANAL")
            .because("o mesmo núcleo atende todos os canais; formatação, token e contrato são do canal."
                    + " Corrija: devolva dados crus do use case e trate no módulo do canal;"
                    + " regra de negócio que muda por canal vai para o domínio (CanalDeAtendimento)");
}
