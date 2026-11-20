package cucumber.passos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Os passos das especificações do processo de pagamento. Só HTTP: nenhuma classe de produção. Cada cenário ganha uma
 * instância nova (o processo do cenário e a última resposta ficam aqui). Depois de cada ação, o efeito é conferido por
 * outro endpoint: a situação gravada, as ações disponíveis, o histórico.
 */
public class PassosDoProcesso {

    /** As sequências recomeçam aqui (R__massa_aceitacao.sql): abaixo, massa; daqui para cima, criado pelos cenários. */
    static final long PRIMEIRO_ID_GERADO = 1000;
    /** O processo 1 é a sentinela, que o runner confere antes de tudo: nenhum cenário o usa. */
    static final long SENTINELA = 1;
    /** Que cenário usa cada processo da massa: dois cenários no mesmo registro dependeriam da ordem. */
    private static final Map<Long, String> DONO_DA_MASSA = new ConcurrentHashMap<>();

    private String cenario = "";
    private Long processo;
    private Response ultima;

    @Before
    public void cenario(Scenario atual) {
        cenario = atual.getUri() + ":" + atual.getLine();
    }

    // ---------- Dado ----------

    @Dado("o processo {long} da massa, de {tipo}, na situação {situacao}")
    public void processoDaMassa(long id, String tipo, String situacao) {
        reservar(id);
        processo = id;
        JsonPath gravado = consultar();
        assertEquals(tipo, gravado.getString("tipo"), "tipo do processo " + id + " na massa");
        assertEquals(situacao, gravado.getString("estado"), "situação do processo " + id + " na massa");
    }

    @Dado("o processo {long} da massa, de {tipo}, na situação {situacao}, com valor de {valor}")
    public void processoDaMassaComValor(long id, String tipo, String situacao, BigDecimal valor) {
        processoDaMassa(id, tipo, situacao);
        assertEquals(0, valor.compareTo(consultar().getObject("valor", BigDecimal.class)), "valor do processo " + id + " na massa");
    }

    @Dado("um processo novo de {tipo}, da cota {string}, com valor de {valor}")
    public void processoNovo(String tipo, String cota, BigDecimal valor) {
        abrir(tipo, cota, valor);
        assertEquals(201, ultima.statusCode(), () -> "abrir o processo: " + ultima.asString());
        processo = ultima.jsonPath().getLong("id");
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

    @Quando("{string} {acao}")
    public void executar(String usuario, String acao) {
        ultima = RestAssured.given().header("X-Usuario", usuario)
                .when().post("/processos/{id}/acoes/{acao}", processoDoCenario(), acao);
    }

    @Quando("{string} {acao} com o motivo {string}")
    public void executarComMotivo(String usuario, String acao, String motivo) {
        ultima = RestAssured.given().header("X-Usuario", usuario).contentType(ContentType.JSON)
                .body(Map.of("motivo", motivo))
                .when().post("/processos/{id}/acoes/{acao}", processoDoCenario(), acao);
    }

    // ---------- Então ----------

    @Entao("o processo é aberto com o número {long}")
    public void abertoComNumero(long numero) {
        assertEquals(201, ultima.statusCode(), () -> "abrir o processo: " + ultima.asString());
        assertEquals(numero, ultima.jsonPath().getLong("id"), "número do processo aberto");
    }

    @Entao("a abertura é recusada")
    public void aberturaRecusada() {
        assertEquals(400, ultima.statusCode(), () -> "abrir o processo: " + ultima.asString());
    }

    @Entao("o processo fica {situacao}")
    public void processoFica(String situacao) {
        assertTrue(ultima.statusCode() / 100 == 2, () -> "a ação foi recusada: " + ultima.statusCode() + " " + ultima.asString());
        processoContinua(situacao);
    }

    @Entao("a ação é recusada com a mensagem {string}")
    public void acaoRecusada(String mensagem) {
        assertTrue(ultima.statusCode() == 400 || ultima.statusCode() == 409,
                () -> "esperava a recusa, mas veio " + ultima.statusCode() + " " + ultima.asString());
        assertEquals(mensagem, ultima.jsonPath().getString("mensagem"));
    }

    @Entao("a ação é recusada")
    public void acaoRecusadaSemMensagem() {
        assertTrue(ultima.statusCode() == 400 || ultima.statusCode() == 409,
                () -> "esperava a recusa, mas veio " + ultima.statusCode() + " " + ultima.asString());
    }

    @E("o processo continua {situacao}")
    public void processoContinua(String situacao) {
        assertEquals(situacao, consultar().getString("estado"), "situação gravada do processo " + processo);
    }

    @E("as ações disponíveis são:")
    public void acoesDisponiveis(List<String> termos) {
        Set<String> esperadas = new HashSet<>();
        termos.forEach(t -> esperadas.add(Vocabulario.codigo(Vocabulario.ACOES, "ação", t)));
        List<String> atuais = RestAssured.given().when().get("/processos/{id}/acoes", processoDoCenario())
                .then().statusCode(200).extract().jsonPath().getList("$", String.class);
        assertEquals(esperadas, new HashSet<>(atuais), "ações disponíveis do processo " + processo);
    }

    @E("nenhuma ação está disponível")
    public void nenhumaAcao() {
        acoesDisponiveis(List.of());
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

    private void reservar(long id) {
        if (id == SENTINELA) {
            fail("O processo " + id + " é a sentinela do banco da aceitação: nenhum cenário o usa. Crie outro na massa.");
        }
        if (id >= PRIMEIRO_ID_GERADO) {
            fail("O processo " + id + " não é da massa (os da massa ficam abaixo de " + PRIMEIRO_ID_GERADO + ").");
        }
        String dono = DONO_DA_MASSA.putIfAbsent(id, cenario);
        if (dono != null && !dono.equals(cenario)) {
            fail("O processo " + id + " da massa já é usado pelo cenário " + dono
                    + ": cada cenário usa o seu, senão o resultado depende da ordem. Crie outro na massa.");
        }
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
