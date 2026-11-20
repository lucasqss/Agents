package br.com.claudio.pagamento.aceitacao;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Banco da aceitação: um PostgreSQL novo a cada execução, com as migrations de produção (db/migration) e, por cima, a
 * massa de teste (db/massa/R__massa_aceitacao.sql), que grava os registros com IDs fixos e reinicia as sequências em 1000.
 * Resultado: em todo build, a massa tem os mesmos IDs e o que os cenários criam ganha 1000, 1001... na ordem dos cenários.
 *
 * <p>Por que aqui e não numa pasta a mais do Flyway da aplicação: {@code quarkus.flyway.locations} é fixado no build, e o
 * jar do *IT não leva o classpath de teste. Este recurso roda na JVM do teste, migra antes de a aplicação subir e entrega a
 * ela só o endereço do banco; vale igual no *Test e no *IT. Genérico: não conhece nenhuma tabela.
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
        // 2) A massa, com histórico próprio: a aplicação não a conhece e não estranha uma migration que não é dela.
        Flyway.configure()
                .dataSource(banco.getJdbcUrl(), banco.getUsername(), banco.getPassword())
                .locations("classpath:db/massa")
                .table("flyway_historico_massa")
                .baselineOnMigrate(true)
                .load()
                .migrate();
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
