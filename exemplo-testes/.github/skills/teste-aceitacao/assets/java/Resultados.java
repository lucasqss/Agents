package PACOTE_RAIZ.aceitacao;

import io.cucumber.plugin.ConcurrentEventListener;
import io.cucumber.plugin.event.EventPublisher;
import io.cucumber.plugin.event.Status;
import io.cucumber.plugin.event.TestCase;
import io.cucumber.plugin.event.TestCaseFinished;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.jspecify.annotations.Nullable;

/** Plugin do Cucumber que guarda o resultado de cada cenário, para o runner devolvê-los como testes JUnit. */
public final class Resultados implements ConcurrentEventListener {

    record Cenario(String nome, Status status, @Nullable Throwable erro) {
    }

    private static final List<Cenario> CENARIOS = new CopyOnWriteArrayList<>();

    public Resultados() {
    }

    static void limpar() {
        CENARIOS.clear();
    }

    static List<Cenario> todos() {
        return List.copyOf(CENARIOS);
    }

    @Override
    public void setEventPublisher(EventPublisher publicador) {
        publicador.registerHandlerFor(TestCaseFinished.class, evento -> CENARIOS.add(new Cenario(
                nome(evento.getTestCase()), evento.getResult().getStatus(), evento.getResult().getError())));
    }

    private static String nome(TestCase cenario) {
        String caminho = cenario.getUri().toString();
        return caminho.substring(caminho.lastIndexOf('/') + 1) + " | " + cenario.getName();
    }
}
