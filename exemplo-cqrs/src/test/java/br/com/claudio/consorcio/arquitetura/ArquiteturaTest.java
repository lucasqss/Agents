package br.com.claudio.consorcio.arquitetura;

import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Fitness functions da arquitetura. Este é o único arquivo que conhece o pacote raiz: as regras usam só
 * padrões relativos ("..command.domain.."), então a pasta {@code arquitetura} pode ser copiada para outro
 * projeto trocando apenas o pacote raiz em {@code @AnalyzeClasses} e em {@link #semCiclos}.
 */
@AnalyzeClasses(packages = "br.com.claudio.consorcio", importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchTests dominio = ArchTests.in(RegrasDominio.class);

    @ArchTest
    static final ArchTests command = ArchTests.in(RegrasCommand.class);

    @ArchTest
    static final ArchTests query = ArchTests.in(RegrasQuery.class);

    @ArchTest
    static final ArchTests shared = ArchTests.in(RegrasShared.class);

    @ArchTest
    static final ArchTests canais = ArchTests.in(RegrasCanais.class);

    @ArchTest
    static final ArchTests tecnologia = ArchTests.in(RegrasTecnologia.class);

    @ArchTest
    static final ArchTests nomes = ArchTests.in(RegrasNomes.class);

    /** Sem ciclos entre os pacotes de segundo nível (command.domain, query.persistence, shared.gateway...). */
    @ArchTest
    static final ArchRule semCiclos = slices()
            .matching("br.com.claudio.consorcio.(*).(*)..")
            .should().beFreeOfCycles();
}
