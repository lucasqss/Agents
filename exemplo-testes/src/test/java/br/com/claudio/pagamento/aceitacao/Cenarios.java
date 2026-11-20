package br.com.claudio.pagamento.aceitacao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Confere os cenários antes de qualquer requisição: os IDs só são reprodutíveis se cada processo da massa for usado por
 * um cenário só (outro cenário o encontraria em outro estado, conforme a ordem) e se o ID citado for da massa (abaixo de
 * {@link #PRIMEIRO_ID_GERADO}). Genérica: confere todo campo de passo com o nome dado.
 */
public final class Cenarios {

    /** As sequências recomeçam aqui (R__massa_aceitacao.sql): abaixo, massa; daqui para cima, criado pelos cenários. */
    public static final long PRIMEIRO_ID_GERADO = 1000;

    private Cenarios() {
    }

    /**
     * Valida as referências à massa e devolve os cenários como vieram.
     *
     * @param cenarios os cenários de cenarios.yaml, cada um com "cenario" e a lista "passos"
     * @param campo o campo dos passos que cita um registro da massa pelo ID (ex.: "processo")
     * @return os mesmos cenários, para o Examples do Karate
     */
    public static List<Map<String, Object>> validar(List<Map<String, Object>> cenarios, String campo) {
        List<String> erros = new ArrayList<>();
        Map<Long, Object> dono = new HashMap<>();
        for (Map<String, Object> cenario : cenarios) {
            Object nome = cenario.get("cenario");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> passos = (List<Map<String, Object>>) cenario.get("passos");
            for (int i = 0; i < passos.size(); i++) {
                Object valor = passos.get(i).get(campo);
                if (valor == null) {
                    continue;
                }
                long id = ((Number) valor).longValue();
                String onde = "cenarios.yaml, cenário " + nome + ", passo " + (i + 1) + ": " + campo + " " + id;
                if (id >= PRIMEIRO_ID_GERADO) {
                    erros.add(onde + " não é da massa (IDs da massa ficam abaixo de " + PRIMEIRO_ID_GERADO + ")");
                }
                Object outro = dono.putIfAbsent(id, nome);
                if (outro != null && !outro.equals(nome)) {
                    erros.add(onde + " já é usado pelo cenário " + outro + ": crie outro registro na massa");
                }
            }
        }
        if (!erros.isEmpty()) {
            throw new IllegalStateException(String.join("\n", erros));
        }
        return cenarios;
    }
}
