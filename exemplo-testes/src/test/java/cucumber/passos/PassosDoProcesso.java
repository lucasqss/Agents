package cucumber.passos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import cucumber.massa.MassaDeProcessos;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.pt.Dado;
import io.cucumber.java.pt.E;
import io.cucumber.java.pt.Entao;
import io.cucumber.java.pt.Quando;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * As frases das especificações do processo de pagamento. O QA preenche tabelas (a massa em
 * {@code cucumber/massa/processos.md} e as colunas dos {@code Exemplos}); estas frases leem cada linha e a executam. Só
 * HTTP: nenhuma classe de produção. Cada cenário ganha uma instância nova. Célula vazia quer dizer "não conferir".
 */
public class PassosDoProcesso {

    /** Que cenário usa cada processo da massa: dois cenários no mesmo processo dependeriam da ordem. */
    private static final Map<String, String> DONO_DA_COTA = new ConcurrentHashMap<>();

    private String cenario = "";
    private Long processo;
    private Response ultima;

    @Before
    public void cenario(Scenario atual) {
        cenario = atual.getUri() + ":" + atual.getLine();
    }

    // ---------- Dado ----------

    /** Parte de um processo da massa, citado pela cota. Confere que ele está como a massa diz. */
    @Dado("o processo da cota {string} da massa")
    public void processoDaMassa(String cota) {
        MassaDeProcessos.Processo daMassa = MassaDeProcessos.daCota(cota);
        String dono = DONO_DA_COTA.putIfAbsent(daMassa.cota(), cenario);
        if (dono != null && !dono.equals(cenario)) {
            fail("A cota " + cota + " da massa já é usada pelo cenário " + dono
                    + ": cada cenário usa o seu processo, senão o resultado depende da ordem. Crie outra linha na massa.");
        }
        processo = daMassa.id();
        JsonPath gravado = consultar();
        assertEquals(Vocabulario.termo(Vocabulario.SITUACOES, daMassa.situacao()), situacaoDe(gravado),
                "situação gravada do processo da cota " + cota + ", que a massa diz (" + daMassa.onde() + ")");
    }

    @Dado("um processo novo de {tipo}, da cota {string}, com valor de {valor}")
    public void processoNovo(String tipo, String cota, BigDecimal valor) {
        abrir(tipo, cota, valor);
        assertEquals(201, ultima.statusCode(), () -> "abrir o processo: " + ultima.asString());
    }

    // ---------- Quando ----------

    @Quando("alguém abre um processo de {tipo}, da cota {string}, com valor de {valor}")
    public void abrir(String tipo, String cota, BigDecimal valor) {
        ultima = RestAssured.given().contentType(ContentType.JSON)
                .body(Map.of("tipo", tipo, "numeroCota", cota, "valor", valor))
                .when().post("/processos");
        if (ultima.statusCode() == 201) {
            processo = ultima.jsonPath().getLong("id");
        }
    }

    /** Motivo vazio: a ação vai sem corpo (e as que exigem motivo são recusadas). */
    @Quando("{string} pedir para {string} com o motivo {string}")
    public void executar(String quem, String acao, String motivo) {
        RequestSpecification pedido = RestAssured.given().header("X-Usuario", quem);
        if (!motivo.isBlank()) {
            pedido = pedido.contentType(ContentType.JSON).body(Map.of("motivo", motivo));
        }
        ultima = pedido.when().post("/processos/{id}/acoes/{acao}", processoDoCenario(),
                Vocabulario.codigo(Vocabulario.ACOES, "ação", acao));
    }

    /** Vários passos sobre o mesmo processo, uma linha por ação: quem, ação, motivo, resultado, situação (e mensagem). */
    @Quando("os passos abaixo são executados em ordem:")
    public void passos(DataTable tabela) {
        List<Map<String, String>> linhas = tabela.asMaps();
        for (int i = 0; i < linhas.size(); i++) {
            Map<String, String> passo = linhas.get(i);
            String onde = "passo " + (i + 1) + " (" + valor(passo, "ação") + ")";
            try {
                executar(valor(passo, "quem"), valor(passo, "ação"), valor(passo, "motivo"));
                acaoE(valor(passo, "resultado"), valor(passo, "situação"));
                mensagem(valor(passo, "mensagem"));
            } catch (AssertionError | IllegalArgumentException e) {
                throw new AssertionError(onde + ": " + e.getMessage(), e);
            }
        }
    }

    // ---------- Então ----------

    /** Abertura aceita (201) ou recusada (400); número e situação vazios não são conferidos. */
    @Entao("a abertura é {string}, com número {string} e situação {string}")
    public void abertura(String resultado, String numero, String situacao) {
        conferirResultado(resultado, 201, 400);
        if (!numero.isBlank()) {
            assertEquals(Long.parseLong(numero.strip()), ultima.jsonPath().getLong("id"), "número do processo aberto");
        }
        if (!situacao.isBlank()) {
            conferirSituacao(situacao);
        }
    }

    @Entao("o processo é aberto com o número {long}")
    public void abertoComNumero(long numero) {
        assertEquals(201, ultima.statusCode(), () -> "abrir o processo: " + ultima.asString());
        assertEquals(numero, ultima.jsonPath().getLong("id"), "número do processo aberto");
    }

    /** Ação aceita (2xx) ou recusada (400/409), e a situação gravada depois dela (vazia: não confere). */
    @Entao("a ação é {string} e o processo fica {string}")
    public void acaoE(String resultado, String situacao) {
        conferirResultado(resultado, 200, 400, 409);
        if (!situacao.isBlank()) {
            conferirSituacao(situacao);
        }
    }

    @E("a mensagem é {string}")
    public void mensagem(String mensagem) {
        if (!mensagem.isBlank()) {
            assertEquals(mensagem, ultima.jsonPath().getString("mensagem"));
        }
    }

    /** Ações separadas por vírgula, ou "nenhuma"; vazio não confere. */
    @E("as ações disponíveis são {string}")
    public void acoesDisponiveis(String acoes) {
        if (acoes.isBlank()) {
            return;
        }
        Set<String> esperadas = new HashSet<>();
        if (!"nenhuma".equalsIgnoreCase(acoes.strip())) {
            Arrays.stream(acoes.split(",")).forEach(a -> esperadas.add(Vocabulario.codigo(Vocabulario.ACOES, "ação", a)));
        }
        List<String> atuais = RestAssured.given().when().get("/processos/{id}/acoes", processoDoCenario())
                .then().statusCode(200).extract().jsonPath().getList("$", String.class);
        assertEquals(esperadas, new HashSet<>(atuais), "ações disponíveis do processo " + processo);
    }

    @E("o histórico tem {int} registro(s)")
    public void historicoTem(int quantidade) {
        assertEquals(quantidade, historico().size(), "registros no histórico do processo " + processo);
    }

    /** Tabela vertical: ação, quem, de, para, motivo (só as linhas que o cenário quer conferir). */
    @E("o último registro do histórico é:")
    public void ultimoRegistro(DataTable tabela) {
        List<Map<String, Object>> historico = historico();
        assertTrue(!historico.isEmpty(), "histórico vazio");
        Map<String, Object> ultimo = historico.getLast();
        tabela.asMap().forEach((campo, valor) -> {
            switch (campo) {
                case "ação" -> assertEquals(Vocabulario.codigo(Vocabulario.ACOES, "ação", valor), ultimo.get("acao"));
                case "quem" -> assertEquals(valor, ultimo.get("autor"));
                case "de" -> assertEquals(Vocabulario.codigo(Vocabulario.SITUACOES, "situação", valor), ultimo.get("de"));
                case "para" -> assertEquals(Vocabulario.codigo(Vocabulario.SITUACOES, "situação", valor), ultimo.get("para"));
                case "motivo" -> assertEquals(valor, ultimo.get("observacao"));
                default -> fail("Campo do histórico desconhecido: '" + campo + "'. Use: ação, quem, de, para, motivo");
            }
        });
    }

    // ---------- apoio ----------

    /** "aceita": o primeiro status; "recusada": um dos demais. */
    private void conferirResultado(String resultado, int aceita, int... recusada) {
        int status = ultima.statusCode();
        switch (resultado.strip()) {
            case "aceita" -> assertTrue(status == aceita || status / 100 == 2,
                    () -> "esperava a ação aceita, mas foi recusada: " + status + " " + ultima.asString());
            case "recusada" -> assertTrue(Arrays.stream(recusada).anyMatch(s -> s == status),
                    () -> "esperava a recusa, mas veio " + status + " " + ultima.asString());
            default -> throw new IllegalArgumentException("Resultado desconhecido: '" + resultado + "'. Use: aceita, recusada");
        }
    }

    /** Compara pelos termos do negócio: a falha diz "esperado Aprovado, gravado Aguardando alçada". */
    private void conferirSituacao(String situacao) {
        String esperada = Vocabulario.termo(Vocabulario.SITUACOES, Vocabulario.codigo(Vocabulario.SITUACOES, "situação", situacao));
        assertEquals(esperada, situacaoDe(consultar()), "situação gravada do processo");
    }

    private static String situacaoDe(JsonPath processo) {
        return Vocabulario.termo(Vocabulario.SITUACOES, processo.getString("estado"));
    }

    private static String valor(Map<String, String> linha, String coluna) {
        String valor = linha.get(coluna);
        return valor == null ? "" : valor;
    }

    private Long processoDoCenario() {
        assertNotNull(processo, "o cenário ainda não tem processo: comece com um passo 'Dado'");
        return processo;
    }

    private JsonPath consultar() {
        return RestAssured.given().when().get("/processos/{id}", processoDoCenario())
                .then().statusCode(200).extract().jsonPath();
    }

    private List<Map<String, Object>> historico() {
        return RestAssured.given().when().get("/processos/{id}/historico", processoDoCenario())
                .then().statusCode(200).extract().jsonPath().getList("$");
    }
}
