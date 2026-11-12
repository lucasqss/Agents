package PACOTE_RAIZ.arquitetura;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.LocationProvider;
import java.util.Set;

/**
 * Pacote raiz do projeto, descoberto pelo lugar desta pasta: ela fica sempre em {@code <raiz>.arquitetura}.
 * Para levar as regras a outro projeto, copie a pasta para {@code src/test/java/<pacote raiz>/arquitetura/}
 * (a IDE ajusta a linha {@code package}) e ajuste {@link ClassesExternas}; nada mais muda.
 *
 * <p>Todos os padrões das regras são ancorados aqui. Assim, {@code "domain"} só casa com o nosso
 * {@code <raiz>.domain}, nunca com pacotes de bibliotecas.
 */
public final class Raiz implements LocationProvider {

    private static final String PASTA = ".arquitetura";

    /** Ex.: com.empresa.produto */
    public static final String PACOTE = descobrir();

    static {
        // Regras de pastas que o projeto ainda não tem (sem canal, sem gateway...) passam vazias.
        // GER-001 (RegrasGerais) garante que as classes do projeto foram mesmo encontradas.
        ArchConfiguration.get().setProperty("archRule.failOnEmptyShould", "false");
        // Mensagem de falha legível: regra, por quê, como corrigir e onde (veja FormatoDaFalha).
        ArchConfiguration.get().setProperty("failureDisplayFormat", FormatoDaFalha.class.getName());
    }

    public Raiz() {
    }

    @Override
    public Set<Location> get(Class<?> testClass) {
        return Locations.ofPackage(PACOTE);
    }

    /** {@code pacote("domain")} = {@code <raiz>.domain..} (o pacote e os subpacotes). */
    static String pacote(String relativo) {
        return PACOTE + "." + relativo + "..";
    }

    /** {@code exato("entrypoint.apresentacao")} = só esse pacote, sem os subpacotes. */
    static String exato(String relativo) {
        return PACOTE + "." + relativo;
    }

    /** {@code emQualquer("entrypoint")} = {@code <raiz>..entrypoint..}: o entrypoint de qualquer lado. */
    static String emQualquer(String segmento) {
        return PACOTE + ".." + segmento + "..";
    }

    private static String descobrir() {
        String pacote = Raiz.class.getPackageName();
        if (!pacote.endsWith(PASTA)) {
            throw new IllegalStateException("A pasta de regras precisa ficar em <pacote raiz>" + PASTA
                    + ", mas está em " + pacote);
        }
        return pacote.substring(0, pacote.length() - PASTA.length());
    }
}
