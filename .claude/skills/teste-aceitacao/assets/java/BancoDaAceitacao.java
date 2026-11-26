package cucumber;

import cucumber.massa.MassaDeProcessos;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Banco da aceitação: um PostgreSQL novo a cada execução, com as migrations de produção (db/migration) e, por cima, a
 * massa que o QA preenche ({@code cucumber/massa/processos.md}), validada e gravada por {@link MassaDeProcessos}.
 * Resultado: em todo build, a massa tem os mesmos IDs e o que os cenários criam ganha 1000, 1001... na ordem dos cenários.
 *
 * <p>Por que aqui e não numa pasta a mais do Flyway da aplicação: {@code quarkus.flyway.locations} é fixado no build, e o
 * jar do *IT não leva o classpath de teste. Este recurso roda na JVM do teste, prepara o banco antes de a aplicação subir
 * e entrega a ela só o endereço do banco; vale igual no *Test e no *IT.
 */
public class BancoDaAceitacao implements QuarkusTestResourceLifecycleManager {

    private static final String IMAGEM = "postgres:17-alpine";

    private PostgreSQLContainer banco;

    @Override
    public Map<String, String> start() {
        banco = new PostgreSQLContainer(IMAGEM);
        banco.start();
        // 1) O schema, com o mesmo histórico que a aplicação usa: ao subir, ela confere e não tem nada a migrar.
        Flyway.configure()
                .dataSource(banco.getJdbcUrl(), banco.getUsername(), banco.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
        // 2) A massa do QA. Arquivo inválido para aqui, dizendo a linha, antes de a aplicação subir.
        try (Connection conexao = DriverManager.getConnection(banco.getJdbcUrl(), banco.getUsername(), banco.getPassword())) {
            MassaDeProcessos.carregar(conexao);
        } catch (SQLException e) {
            throw new IllegalStateException("Não foi possível gravar a massa da aceitação: " + e.getMessage(), e);
        }
        // Só o endereço do banco. Nada que mude o comportamento da aplicação: no *IT esta configuração vale também para
        // os jars que sobem depois, nos outros *IT, e eles têm de se comportar como em produção.
        return Map.of(
                "quarkus.datasource.jdbc.url", banco.getJdbcUrl(),
                "quarkus.datasource.username", banco.getUsername(),
                "quarkus.datasource.password", banco.getPassword());
    }

    @Override
    public void stop() {
        if (banco != null) {
            banco.stop();
        }
    }
}
