package PACOTE_RAIZ.aceitacao;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê um CSV de massa do classpath, do lado Java (o simulador de sistema externo usa esta classe; o Karate lê os CSV
 * sozinho). UTF-8, primeira linha com os nomes das colunas, separador vírgula, valor com vírgula entre aspas duplas
 * ({@code "bom pagador, sem restrição"}) e aspas dentro de aspas dobradas ({@code ""}).
 */
public final class Massa {

    private Massa() {
    }

    public static List<Map<String, String>> ler(String recurso) {
        try (InputStream entrada = Massa.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalArgumentException("Massa não encontrada no classpath: " + recurso);
            }
            List<String> linhas = new String(entrada.readAllBytes(), StandardCharsets.UTF_8).lines()
                    .filter(l -> !l.isBlank()).toList();
            List<String> colunas = campos(linhas.getFirst());
            List<Map<String, String>> registros = new ArrayList<>();
            for (String linha : linhas.subList(1, linhas.size())) {
                List<String> valores = campos(linha);
                Map<String, String> registro = new LinkedHashMap<>();
                for (int i = 0; i < colunas.size(); i++) {
                    registro.put(colunas.get(i).strip(), i < valores.size() ? valores.get(i).strip() : "");
                }
                registros.add(registro);
            }
            return registros;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static List<String> campos(String linha) {
        List<String> campos = new ArrayList<>();
        StringBuilder atual = new StringBuilder();
        boolean entreAspas = false;
        for (int i = 0; i < linha.length(); i++) {
            char c = linha.charAt(i);
            if (entreAspas && c == '"' && i + 1 < linha.length() && linha.charAt(i + 1) == '"') {
                atual.append('"');
                i++;
            } else if (c == '"') {
                entreAspas = !entreAspas;
            } else if (c == ',' && !entreAspas) {
                campos.add(atual.toString());
                atual.setLength(0);
            } else {
                atual.append(c);
            }
        }
        campos.add(atual.toString());
        return campos;
    }
}
