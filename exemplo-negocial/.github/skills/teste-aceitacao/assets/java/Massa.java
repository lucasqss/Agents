package PACOTE_RAIZ.aceitacao;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Lê uma massa YAML do classpath, do lado Java (o simulador de sistema externo usa esta classe; o Karate lê os YAML
 * sozinho). O arquivo é uma lista de registros; os tipos (texto, número, verdadeiro/falso, lista) vêm do YAML.
 */
public final class Massa {

    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory());

    private Massa() {
    }

    public static List<Map<String, Object>> ler(String recurso) {
        try (InputStream entrada = Massa.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalArgumentException("Massa não encontrada no classpath: " + recurso);
            }
            return YAML.readValue(entrada, new TypeReference<List<Map<String, Object>>>() { });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
