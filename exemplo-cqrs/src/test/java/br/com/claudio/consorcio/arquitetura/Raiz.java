package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.ArchConfiguration;
import com.tngtech.archunit.core.importer.Location;
import com.tngtech.archunit.core.importer.Locations;
import com.tngtech.archunit.junit.LocationProvider;
import java.util.Set;

/**
 * Pacote raiz do projeto, descoberto pelo lugar desta pasta: ela fica sempre em {@code <raiz>.arquitetura}.
 * Para levar as regras a outro projeto, copie a pasta para {@code src/test/java/<pacote raiz>/arquitetura/}
 * (a IDE ajusta a linha {@code package}); nada mais muda.
 *
 * <p>Todos os padrões das regras são ancorados aqui. Assim, {@code "query"} só casa com o nosso
 * {@code <raiz>.query}, nunca com pacotes de bibliotecas como {@code org.hibernate.query}.
 */
public final class Raiz implements LocationProvider {

    private static final String PASTA = ".arquitetura";

    /** Ex.: com.empresa.produto */
    public static final String PACOTE = descobrir();

    static {
        // Regras de pastas que o projeto ainda não tem (sem canal, sem gateway...) passam vazias.
        // RegrasGerais.estrutura garante que as classes do projeto foram mesmo encontradas.
        ArchConfiguration.get().setProperty("archRule.failOnEmptyShould", "false");
    }

    public Raiz() {
    }

    @Override
    public Set<Location> get(Class<?> testClass) {
        return Locations.ofPackage(PACOTE);
    }

    /** {@code pacote("command.domain")} = {@code <raiz>.command.domain..} (o pacote e os subpacotes). */
    static String pacote(String relativo) {
        return PACOTE + "." + relativo + "..";
    }

    /** {@code exato("shared.apresentacao")} = só esse pacote, sem os subpacotes. */
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
