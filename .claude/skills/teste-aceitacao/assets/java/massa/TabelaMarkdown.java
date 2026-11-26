package cucumber.massa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lê a tabela de um arquivo Markdown do classpath: as linhas que começam com {@code |}, a primeira delas como cabeçalho.
 * A linha separadora ({@code |---|}) e o texto em volta (explicações ao QA) são ignorados. Genérica: serve para qualquer
 * massa em tabela.
 */
public final class TabelaMarkdown {

    /** Uma linha da tabela: o número dela no arquivo (para as mensagens) e o valor de cada coluna. */
    public record Linha(String arquivo, int numero, Map<String, String> colunas) {

        public String valor(String coluna) {
            return colunas.getOrDefault(coluna, "");
        }

        public String onde() {
            return arquivo + ", linha " + numero;
        }
    }

    private TabelaMarkdown() {
    }

    public static List<Linha> ler(String recurso, List<String> colunasObrigatorias) {
        List<String> cabecalho = null;
        List<Linha> linhas = new ArrayList<>();
        try (InputStream entrada = TabelaMarkdown.class.getClassLoader().getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("Massa não encontrada no classpath: " + recurso);
            }
            BufferedReader leitor = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8));
            String texto;
            int numero = 0;
            while ((texto = leitor.readLine()) != null) {
                numero++;
                String linha = texto.strip();
                if (!linha.startsWith("|") || linha.matches("\\|[\\s|:-]*\\|")) {
                    continue;
                }
                List<String> celulas = celulas(linha);
                if (cabecalho == null) {
                    cabecalho = celulas;
                    for (String obrigatoria : colunasObrigatorias) {
                        if (!cabecalho.contains(obrigatoria)) {
                            throw new IllegalStateException(recurso + ", linha " + numero + ": falta a coluna '"
                                    + obrigatoria + "'. Colunas obrigatórias: " + colunasObrigatorias);
                        }
                    }
                    continue;
                }
                if (celulas.size() != cabecalho.size()) {
                    throw new IllegalStateException(recurso + ", linha " + numero + ": a linha tem " + celulas.size()
                            + " colunas e o cabeçalho tem " + cabecalho.size());
                }
                Map<String, String> colunas = new LinkedHashMap<>();
                for (int i = 0; i < cabecalho.size(); i++) {
                    colunas.put(cabecalho.get(i), celulas.get(i));
                }
                linhas.add(new Linha(recurso, numero, colunas));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (cabecalho == null) {
            throw new IllegalStateException(recurso + ": nenhuma tabela encontrada");
        }
        return List.copyOf(linhas);
    }

    private static List<String> celulas(String linha) {
        String miolo = linha.substring(1, linha.endsWith("|") ? linha.length() - 1 : linha.length());
        return Arrays.stream(miolo.split("\\|", -1)).map(String::strip).toList();
    }
}
