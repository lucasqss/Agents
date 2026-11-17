package br.com.claudio.consorcio;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Nenhum campo de resposta publicado some ou muda de tipo (AGENTS.md: só mudanças aditivas; quebra vira /v2).
 * Compara o OpenAPI atual com a linha de base versionada em {@code docs/api/openapi-base.json}. Complementa o
 * openapi-diff da fase verify, que só trata como quebra a remoção de campo marcado como obrigatório, e o Quarkus não
 * marca os campos dos records de resposta. Mudança aditiva aceita: atualize a linha de base no mesmo commit, com
 * {@code ./mvnw package -DskipTests && cp target/openapi/openapi.json docs/api/openapi-base.json}.
 */
@QuarkusTest
class ContratoRestTest {

    private static final Path LINHA_DE_BASE = Path.of("docs/api/openapi-base.json");
    private static final String PREFIXO_REF = "#/components/schemas/";

    private final ObjectMapper json = new ObjectMapper();

    @Test
    void camposDasRespostasPublicadasContinuamExistindoComOMesmoTipo() throws IOException {
        JsonNode base = json.readTree(LINHA_DE_BASE.toFile());
        JsonNode atual = json.readTree(given().queryParam("format", "json").when().get("/q/openapi").then()
                .statusCode(200).extract().asString());

        List<String> quebras = new ArrayList<>();
        for (String nome : schemasDeResposta(base)) {
            JsonNode antes = base.path("components").path("schemas").path(nome);
            JsonNode depois = atual.path("components").path("schemas").path(nome);
            if (depois.isMissingNode()) {
                quebras.add(nome + ": schema removido ou renomeado");
                continue;
            }
            for (Map.Entry<String, JsonNode> campo : antes.path("properties").properties()) {
                JsonNode novo = depois.path("properties").path(campo.getKey());
                if (novo.isMissingNode()) {
                    quebras.add(nome + "." + campo.getKey() + ": campo removido ou renomeado");
                } else if (!tipo(campo.getValue()).equals(tipo(novo))) {
                    quebras.add(nome + "." + campo.getKey() + ": tipo mudou de " + tipo(campo.getValue()) + " para "
                            + tipo(novo));
                }
            }
        }
        assertTrue(quebras.isEmpty(), () -> "Contrato de resposta quebrado (publique em /v2 em vez de mudar):\n  "
                + String.join("\n  ", quebras));
    }

    /** Schemas que aparecem em alguma resposta, direto ou dentro de outro schema de resposta. */
    private static Set<String> schemasDeResposta(JsonNode openApi) {
        Set<String> alcancados = new LinkedHashSet<>();
        Deque<JsonNode> pendentes = new ArrayDeque<>();
        openApi.path("paths").forEach(caminho -> caminho.forEach(operacao -> pendentes.add(operacao.path("responses"))));
        while (!pendentes.isEmpty()) {
            JsonNode no = pendentes.pop();
            String ref = no.path("$ref").asText("");
            if (ref.startsWith(PREFIXO_REF) && alcancados.add(ref.substring(PREFIXO_REF.length()))) {
                pendentes.add(openApi.path("components").path("schemas").path(ref.substring(PREFIXO_REF.length())));
            }
            no.forEach(pendentes::add);
        }
        return alcancados;
    }

    /** O que define o tipo de um campo: tipo, formato, schema referenciado e o mesmo para os itens de uma lista. */
    private static String tipo(JsonNode campo) {
        String proprio = campo.path("type").toString() + "/" + campo.path("format").asText("") + "/"
                + campo.path("$ref").asText("");
        JsonNode itens = campo.path("items");
        return itens.isMissingNode() ? proprio : proprio + "[" + tipo(itens) + "]";
    }
}
