package PACOTE_RAIZ.arquitetura;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.junit.ArchTests;

/**
 * Fitness functions da arquitetura de um componente ACL. Nenhum arquivo desta pasta tem o nome do projeto: o pacote raiz é
 * descoberto por {@link Raiz}. Para usar em outro projeto, copie a pasta para
 * {@code src/test/java/<pacote raiz>/arquitetura/} e ajuste {@link ClassesExternas}.
 */
@AnalyzeClasses(locations = Raiz.class, importOptions = ImportOption.DoNotIncludeTests.class)
class ArquiteturaTest {

    @ArchTest
    static final ArchTests gerais = ArchTests.in(RegrasGerais.class);

    @ArchTest
    static final ArchTests dominio = ArchTests.in(RegrasDominio.class);

    @ArchTest
    static final ArchTests escrita = ArchTests.in(RegrasEscrita.class);

    @ArchTest
    static final ArchTests leitura = ArchTests.in(RegrasLeitura.class);

    @ArchTest
    static final ArchTests persistenciaEGateway = ArchTests.in(RegrasPersistenciaEGateway.class);

    @ArchTest
    static final ArchTests canais = ArchTests.in(RegrasCanais.class);

    @ArchTest
    static final ArchTests tecnologia = ArchTests.in(RegrasTecnologia.class);

    @ArchTest
    static final ArchTests nomes = ArchTests.in(RegrasNomes.class);

    @ArchTest
    static final ArchTests acl = ArchTests.in(RegrasAcl.class);
}
