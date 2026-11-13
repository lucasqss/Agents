package br.com.claudio.consorcio.entrypoint.apresentacao;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.Function;

/**
 * Ferramentas de formatação, parametrizadas e sem opinião de canal. Concentra os detalhes que costumam
 * dar errado em Java: locale explícito (nunca o da JVM) e o espaço não separável (U+00A0) que o
 * NumberFormat pt-BR coloca depois de "R$". Os padrões de cada canal são montados a partir daqui.
 */
public final class Formatador {

    public static final Locale PT_BR = Locale.of("pt", "BR");

    private static final char ESPACO_NAO_SEPARAVEL = ' ';
    private static final BigDecimal MIL = BigDecimal.valueOf(1_000);
    private static final BigDecimal MILHAO = BigDecimal.valueOf(1_000_000);

    private Formatador() {
    }

    /** Moeda em pt-BR, com o separador informado entre "R$" e o número ("" para colar, " " para espaço comum). */
    public static String moeda(BigDecimal valor, String separador) {
        String texto = NumberFormat.getCurrencyInstance(PT_BR).format(valor);
        return texto.replace(String.valueOf(ESPACO_NAO_SEPARAVEL), separador);
    }

    /** Moeda abreviada: "R$ 250 mil", "R$ 1,2 mi"; abaixo de mil, o valor completo. */
    public static String moedaCompacta(BigDecimal valor) {
        BigDecimal absoluto = valor.abs();
        if (absoluto.compareTo(MILHAO) >= 0) {
            return "R$ " + decimal(valor.divide(MILHAO)) + " mi";
        }
        if (absoluto.compareTo(MIL) >= 0) {
            return "R$ " + decimal(valor.divide(MIL)) + " mil";
        }
        return moeda(valor, " ");
    }

    /**
     * Aplica a máscara a um CPF de 11 dígitos: "#" mostra o dígito, "*" o esconde e os demais caracteres
     * são copiados. Exemplo: "***.###.###-**". Um CPF fora do formato volta como veio.
     */
    public static String cpf(String cpf, String mascara) {
        String digitos = cpf.replaceAll("\\D", "");
        if (digitos.length() != 11) {
            return cpf;
        }
        StringBuilder resultado = new StringBuilder();
        int i = 0;
        for (char c : mascara.toCharArray()) {
            switch (c) {
                case '#' -> resultado.append(digitos.charAt(i++));
                case '*' -> { resultado.append('*'); i++; }
                default -> resultado.append(c);
            }
        }
        return resultado.toString();
    }

    public static String maiusculoSemAcento(String texto) {
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toUpperCase(PT_BR);
    }

    /** Primeiro e último nome: "Júlia Araújo Lima" vira "Júlia Lima". */
    public static String primeiroEUltimoNome(String nome) {
        String[] partes = nome.strip().split("\\s+");
        return partes.length == 1 ? partes[0] : partes[0] + " " + partes[partes.length - 1];
    }

    public static String limitar(String texto, int maximo) {
        return texto.length() <= maximo ? texto : texto.substring(0, maximo).strip();
    }

    public static String quantidade(long n, String singular, String plural) {
        return n + " " + (n == 1 ? singular : plural);
    }

    /** Aplica a função só se o valor não for nulo. */
    public static <T> String seNaoNulo(T valor, Function<T, String> formatar) {
        return valor == null ? null : formatar.apply(valor);
    }

    private static String decimal(BigDecimal valor) {
        DecimalFormat formato = new DecimalFormat("#,##0.#", DecimalFormatSymbols.getInstance(PT_BR));
        formato.setRoundingMode(RoundingMode.HALF_UP);
        return formato.format(valor);
    }
}
