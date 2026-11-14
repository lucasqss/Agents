package PACOTE_RAIZ.aceitacao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Agrupa as linhas de cenarios.csv por cenário e confere, antes de qualquer requisição, que toda coluna que aponta
 * para a massa usa uma chave existente. Genérica: serve para qualquer capacidade.
 */
public final class Tabelas {

    private Tabelas() {
    }

    /**
     * Agrupa as linhas por cenário, depois de conferir as chaves.
     *
     * @param linhas linhas de cenarios.csv, na ordem do arquivo
     * @param massas por coluna de cenarios.csv que aponta para a massa, as linhas dessa massa (com a coluna "chave")
     * @return um item por cenário: as colunas da primeira linha mais "passos", com todas as linhas do cenário
     */
    public static List<Map<String, Object>> agrupar(List<Map<String, Object>> linhas,
            Map<String, List<Map<String, Object>>> massas) {
        List<String> erros = new ArrayList<>();
        massas.forEach((coluna, massa) -> {
            Set<Object> chaves = massa.stream().map(m -> m.get("chave")).collect(Collectors.toSet());
            for (int i = 0; i < linhas.size(); i++) {
                Object valor = linhas.get(i).get(coluna);
                if (valor != null && !valor.toString().isBlank() && !chaves.contains(valor)) {
                    erros.add("cenarios.csv, linha " + (i + 2) + ": " + coluna + " '" + valor + "' não existe na massa");
                }
            }
        });
        if (!erros.isEmpty()) {
            throw new IllegalStateException(String.join("\n", erros));
        }
        Map<Object, Map<String, Object>> porCenario = new LinkedHashMap<>();
        for (Map<String, Object> linha : linhas) {
            Map<String, Object> cenario = porCenario.computeIfAbsent(linha.get("cenario"), c -> {
                Map<String, Object> novo = new LinkedHashMap<>(linha);
                novo.put("passos", new ArrayList<Map<String, Object>>());
                return novo;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> passos = (List<Map<String, Object>>) cenario.get("passos");
            passos.add(linha);
        }
        return new ArrayList<>(porCenario.values());
    }
}
