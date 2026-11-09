package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.HasDescription;
import com.tngtech.archunit.lang.FailureDisplayFormat;
import com.tngtech.archunit.lang.FailureMessages;
import com.tngtech.archunit.lang.Priority;

/**
 * Formata a falha de uma regra para o log do build, a partir de
 * {@code .as("DOM-001: TÍTULO").because("por quê. Corrija: o que fazer")}:
 *
 * <pre>
 * VIOLAÇÃO DE ARQUITETURA (1 ocorrência)
 *   Regra:   DOM-001: DOMÍNIO NÃO PODE DEPENDER DE FRAMEWORK
 *   Por quê: o domínio é Java puro, testável sem subir o Quarkus.
 *   Corrija: tire @Inject/@ApplicationScoped/@Transactional do domínio; ...
 *   Onde:
 *     - Class &lt;...domain.cota.Cota&gt; is annotated with &lt;...ApplicationScoped&gt; in (Cota.java:0)
 * </pre>
 *
 * Ligado em {@link Raiz} pela propriedade {@code failureDisplayFormat} do ArchUnit.
 */
public final class FormatoDaFalha implements FailureDisplayFormat {

    private static final String PORQUE = ", because ";
    private static final String CORRIJA = " Corrija: ";
    private static final String NL = System.lineSeparator();

    public FormatoDaFalha() {
    }

    @Override
    public String formatFailure(HasDescription regra, FailureMessages violacoes, Priority prioridade) {
        String descricao = regra.getDescription();
        String titulo = descricao;
        String porque = null;
        String corrija = null;

        int i = descricao.indexOf(PORQUE);
        if (i >= 0) {
            titulo = descricao.substring(0, i);
            porque = descricao.substring(i + PORQUE.length());
            int j = porque.indexOf(CORRIJA);
            if (j >= 0) {
                corrija = porque.substring(j + CORRIJA.length());
                porque = porque.substring(0, j);
            }
        }

        int total = violacoes.size();
        StringBuilder sb = new StringBuilder()
                .append("VIOLAÇÃO DE ARQUITETURA (").append(total).append(total == 1 ? " ocorrência)" : " ocorrências)")
                .append(NL).append("  Regra:   ").append(titulo);
        if (porque != null) {
            sb.append(NL).append("  Por quê: ").append(porque);
        }
        if (corrija != null) {
            sb.append(NL).append("  Corrija: ").append(corrija);
        }
        sb.append(NL).append("  Onde:");
        for (String violacao : violacoes) {
            sb.append(NL).append("    - ").append(violacao.replace(NL, NL + "      "));
        }
        return sb.append(NL).toString();
    }
}
