package br.com.claudio.consorcio.entrypoint.apresentacao;

import java.util.Map;

/**
 * Catálogo base dos rótulos de negócio, por extenso. Cada canal usa como está, sobrescreve alguns
 * (abreviações do mobile) ou transforma (maiúsculas do TAA). Código desconhecido volta como veio.
 */
public final class Rotulos {

    private static final Map<String, String> ESTADOS = Map.ofEntries(
            Map.entry("ABERTO", "Aberto"),
            Map.entry("EM_ANALISE", "Em análise"),
            Map.entry("PENDENTE_DOCUMENTO", "Pendente de documento"),
            Map.entry("AGUARDANDO_VISTORIA", "Aguardando vistoria"),
            Map.entry("AGUARDANDO_ALCADA", "Aguardando alçada"),
            Map.entry("AGUARDANDO_CONCORDANCIA", "Aguardando concordância"),
            Map.entry("APROVADO", "Aprovado"),
            Map.entry("PAGAMENTO_SOLICITADO", "Pagamento solicitado"),
            Map.entry("PAGO", "Pago"),
            Map.entry("REPROVADO", "Reprovado"),
            Map.entry("CANCELADO", "Cancelado"));

    private static final Map<String, String> TIPOS = Map.of(
            "IMOBILIARIO", "Imobiliário",
            "SERVICO", "Serviço");

    public static final String SEM_PROCESSO = "Sem processo";

    private Rotulos() {
    }

    /** {@code null} significa "sem processo". */
    public static String estado(String codigo) {
        return codigo == null ? SEM_PROCESSO : ESTADOS.getOrDefault(codigo, codigo);
    }

    public static String tipo(String codigo) {
        return TIPOS.getOrDefault(codigo, codigo);
    }

    public static boolean temEstado(String codigo) {
        return ESTADOS.containsKey(codigo);
    }

    public static boolean temTipo(String codigo) {
        return TIPOS.containsKey(codigo);
    }
}
