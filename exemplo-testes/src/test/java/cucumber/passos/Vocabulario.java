package cucumber.passos;

import io.cucumber.java.ParameterType;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * O vocabulário do negócio nas especificações, e o código que a API usa para cada termo. É o único lugar que conhece os
 * dois lados: termo novo (situação, ação, tipo) é uma linha aqui. Termo escrito errado numa especificação falha dizendo
 * os termos válidos.
 */
public class Vocabulario {

    static final Map<String, String> SITUACOES = mapa(
            "Aberto", "ABERTO",
            "Em análise", "EM_ANALISE",
            "Pendente de documento", "PENDENTE_DOCUMENTO",
            "Aguardando vistoria", "AGUARDANDO_VISTORIA",
            "Aguardando alçada", "AGUARDANDO_ALCADA",
            "Aprovado", "APROVADO",
            "Pago", "PAGO",
            "Reprovado", "REPROVADO",
            "Cancelado", "CANCELADO");

    /** A ação escrita como o negócio a diz ("Quando "ana" concluir a análise"). */
    static final Map<String, String> ACOES = mapa(
            "iniciar a análise", "INICIAR_ANALISE",
            "solicitar documento", "SOLICITAR_DOCUMENTO",
            "receber o documento", "RECEBER_DOCUMENTO",
            "concluir a análise", "CONCLUIR_ANALISE",
            "concluir a vistoria", "CONCLUIR_VISTORIA",
            "aprovar na alçada", "APROVAR_NA_ALCADA",
            "devolver para análise", "DEVOLVER_PARA_ANALISE",
            "reprovar", "REPROVAR",
            "pagar", "PAGAR",
            "cancelar", "CANCELAR");

    static final Map<String, String> TIPOS = mapa(
            "serviço", "SERVICO",
            "imobiliário", "IMOBILIARIO");

    /** Situação entre aspas, como o negócio a chama: "Aguardando alçada". Devolve o código da API. */
    @ParameterType("\"([^\"]+)\"")
    public String situacao(String termo) {
        return codigo(SITUACOES, "situação", termo);
    }

    @ParameterType("iniciar a análise|solicitar documento|receber o documento|concluir a análise|concluir a vistoria"
            + "|aprovar na alçada|devolver para análise|reprovar|pagar|cancelar")
    public String acao(String termo) {
        return codigo(ACOES, "ação", termo);
    }

    @ParameterType("serviço|imobiliário")
    public String tipo(String termo) {
        return codigo(TIPOS, "tipo", termo);
    }

    /** Valor em reais, como o negócio escreve: 50.000,01. */
    @ParameterType("\\d{1,3}(?:\\.\\d{3})*,\\d{2}")
    public BigDecimal valor(String texto) {
        return new BigDecimal(texto.replace(".", "").replace(',', '.'));
    }

    static String codigo(Map<String, String> termos, String oQue, String termo) {
        String codigo = termos.get(termo.strip());
        if (codigo == null) {
            throw new IllegalArgumentException("Termo de " + oQue + " desconhecido: '" + termo + "'. Use um destes: "
                    + termos.keySet());
        }
        return codigo;
    }

    private static Map<String, String> mapa(String... termoECodigo) {
        Map<String, String> mapa = new LinkedHashMap<>();
        for (int i = 0; i < termoECodigo.length; i += 2) {
            mapa.put(termoECodigo[i], termoECodigo[i + 1]);
        }
        return mapa;
    }
}
