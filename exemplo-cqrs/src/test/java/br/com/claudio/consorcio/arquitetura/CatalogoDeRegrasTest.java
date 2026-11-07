package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.junit.ArchTests;
import com.tngtech.archunit.lang.ArchRule;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Guarda do catálogo: toda regra tem ID único (ABC-001), título em CAIXA ALTA via .as(...) e motivo via
 * .because(...). Assim, quando uma regra falha, o log do build diz o quê, por quê e como corrigir.
 * Descobre os grupos pelo {@link ArquiteturaTest}: um Regras* novo entra sozinho.
 */
class CatalogoDeRegrasTest {

    private static final Pattern FORMATO =
            Pattern.compile("^([A-Z]{3})-(\\d{3}): ([^a-z]+), because (.+)$", Pattern.DOTALL);

    @Test
    void todaRegraTemIdUnicoTituloEMotivo() throws Exception {
        List<String> erros = new ArrayList<>();
        Map<String, String> ids = new HashMap<>();
        int total = 0;

        for (Field grupo : ArquiteturaTest.class.getDeclaredFields()) {
            if (!ArchTests.class.isAssignableFrom(grupo.getType())) {
                continue;
            }
            grupo.setAccessible(true);
            Class<?> regras = ((ArchTests) grupo.get(null)).getDefinitionLocation();
            String prefixo = null;

            for (Field campo : regras.getDeclaredFields()) {
                if (!ArchRule.class.isAssignableFrom(campo.getType()) || !Modifier.isStatic(campo.getModifiers())) {
                    continue;
                }
                total++;
                campo.setAccessible(true);
                String descricao = ((ArchRule) campo.get(null)).getDescription();
                String onde = regras.getSimpleName() + "." + campo.getName();
                Matcher m = FORMATO.matcher(descricao);
                if (!m.matches()) {
                    erros.add(onde + ": use .as(\"ABC-001: TÍTULO EM CAIXA ALTA\").because(\"motivo. Corrija: ...\")"
                            + " -> hoje: " + descricao);
                    continue;
                }
                String id = m.group(1) + "-" + m.group(2);
                String repetido = ids.put(id, onde);
                if (repetido != null) {
                    erros.add(onde + ": ID " + id + " já usado em " + repetido);
                }
                if (prefixo == null) {
                    prefixo = m.group(1);
                } else if (!prefixo.equals(m.group(1))) {
                    erros.add(onde + ": prefixo " + m.group(1) + " diferente do resto do arquivo (" + prefixo + ")");
                }
                if (!campo.getName().startsWith(m.group(1).toLowerCase() + m.group(2))) {
                    erros.add(onde + ": o nome do campo deve começar com " + m.group(1).toLowerCase() + m.group(2));
                }
            }
        }

        Assertions.assertTrue(total > 0, "nenhuma regra encontrada em ArquiteturaTest");
        Assertions.assertTrue(erros.isEmpty(), "Catálogo de regras inválido:\n  " + String.join("\n  ", erros));
    }
}
