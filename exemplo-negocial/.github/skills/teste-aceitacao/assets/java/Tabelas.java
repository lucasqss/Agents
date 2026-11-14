package PACOTE_RAIZ.aceitacao;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Confere, antes de qualquer requisição, que todo campo de passo que aponta para a massa usa uma chave existente.
 * Genérica: serve para qualquer capacidade.
 */
public final class Tabelas {

    private Tabelas() {
    }

    /**
     * Valida as referências à massa e devolve os cenários como vieram.
     *
     * @param cenarios os cenários de cenarios.yaml, cada um com "cenario" e a lista "passos"
     * @param massas por campo de passo que aponta para a massa, os registros dessa massa (com o campo "chave")
     * @return os mesmos cenários, para o Examples do Karate
     */
    public static List<Map<String, Object>> validar(List<Map<String, Object>> cenarios,
            Map<String, List<Map<String, Object>>> massas) {
        List<String> erros = new ArrayList<>();
        massas.forEach((campo, massa) -> {
            Set<Object> chaves = massa.stream().map(m -> m.get("chave")).collect(Collectors.toSet());
            for (Map<String, Object> cenario : cenarios) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> passos = (List<Map<String, Object>>) cenario.get("passos");
                for (int i = 0; i < passos.size(); i++) {
                    Object valor = passos.get(i).get(campo);
                    if (valor != null && !chaves.contains(valor)) {
                        erros.add("cenarios.yaml, cenário " + cenario.get("cenario") + ", passo " + (i + 1) + ": "
                                + campo + " '" + valor + "' não existe na massa");
                    }
                }
            }
        });
        if (!erros.isEmpty()) {
            throw new IllegalStateException(String.join("\n", erros));
        }
        return cenarios;
    }
}
