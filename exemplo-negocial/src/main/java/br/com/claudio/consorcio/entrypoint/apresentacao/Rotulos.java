package br.com.claudio.consorcio.entrypoint.apresentacao;

import java.util.Map;

/**
 * Catálogo base dos rótulos de negócio, por extenso. Cada canal usa como está, sobrescreve alguns
 * (abreviações do mobile) ou transforma (maiúsculas do TAA). Código desconhecido volta como veio.
 */
public final class Rotulos {

    private static final Map<String, String> ESTADOS = Map.of(
            "ABERTO", "Aberto",
            "EM_ANALISE", "Em análise",
            "PENDENTE_DOCUMENTO", "Pendente de documento",
            "AGUARDANDO_VISTORIA", "Aguardando vistoria",
            "APROVADO", "Aprovado",
            "PAGO", "Pago",
            "REPROVADO", "Reprovado",
            "CANCELADO", "Cancelado");

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
