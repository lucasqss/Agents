package PACOTE_RAIZ.aceitacao;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

/**
 * Barreira: a aceitação depende de rodar em sequência (os IDs gerados seguem a ordem dos cenários, e a aplicação ocupa
 * uma porta só). Se alguém ligar a execução paralela do JUnit, o build falha aqui, dizendo por quê, em vez de os
 * cenários falharem de forma intermitente.
 */
class AceitacaoSequencialTest {

    private static final String PARALELO = "junit.jupiter.execution.parallel.enabled";

    @RegisterExtension
    static final Configuracao CONFIGURACAO = new Configuracao();

    @Test
    void execucaoParalelaDoJunitEstaDesligada() {
        assertFalse(CONFIGURACAO.paraleloLigado, PARALELO + " está ligado: a aceitação precisa rodar em sequência para que os"
                + " IDs gerados pelos cenários (1000, 1001...) sejam os mesmos em todo build. Desligue-o.");
    }

    static final class Configuracao implements BeforeAllCallback {

        boolean paraleloLigado;

        @Override
        public void beforeAll(ExtensionContext contexto) {
            paraleloLigado = contexto.getConfigurationParameter(PARALELO).map(Boolean::parseBoolean).orElse(false);
        }
    }
}
