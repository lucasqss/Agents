package br.com.claudio.smoke;

import com.thoughtworks.gauge.BeforeSuite;
import com.thoughtworks.gauge.Step;
import com.thoughtworks.gauge.Table;
import com.thoughtworks.gauge.TableRow;
import com.thoughtworks.gauge.datastore.ScenarioDataStore;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Implementação das frases de specs/smoke.spec. Cada frase faz chamadas HTTP e confere a resposta. */
public class PassosSmoke {

    @BeforeSuite
    public void aguardarAAplicacao() {
        Api.aguardar(Duration.ofSeconds(Long.parseLong(System.getenv().getOrDefault("ESPERA_API_SEGUNDOS", "120"))));
    }

    @Step("A documentação OpenAPI responde")
    public void documentacaoResponde() {
        Api.Resposta resposta = Api.get("/q/openapi", null);
        confirmar(resposta.status() == 200, "GET /q/openapi deveria responder 200 e respondeu " + resposta.status());
        confirmar(resposta.corpo().contains("openapi:"), "GET /q/openapi não devolveu um documento OpenAPI");
    }

    @Step("Consultar <caminho> sem token responde <status>")
    public void consultarSemToken(String caminho, String status) {
        conferirStatus("GET " + caminho + " sem token", Api.get(caminho, null), status);
    }

    @Step("Com o papel <papel>, cadastrar um cliente responde <status>")
    public void cadastrarClienteComPapel(String papel, String status) {
        Api.Resposta resposta = Api.post("/clientes", Token.para("smoke-" + papel, papel),
                Map.of("nome", "Cliente Recusado", "cpf", unico()));
        conferirStatus("POST /clientes com o papel " + papel, resposta, status);
    }

    @Step("Com o papel <papel>, cadastrar o cliente <nome>")
    public void cadastrarCliente(String papel, String nome) {
        String token = Token.para("smoke-" + papel, papel);
        Api.Resposta resposta = Api.post("/clientes", token, Map.of("nome", nome, "cpf", unico()));
        conferirStatus("POST /clientes", resposta, "201");
        ScenarioDataStore.put("token", token);
        ScenarioDataStore.put("clienteId", resposta.json().path("id").asLong());
    }

    @Step("O cliente cadastrado aparece na lista de clientes")
    public void clienteNaLista() {
        long id = (long) ScenarioDataStore.get("clienteId");
        Api.Resposta resposta = Api.get("/clientes", (String) ScenarioDataStore.get("token"));
        conferirStatus("GET /clientes", resposta, "200");
        List<Long> ids = new ArrayList<>();
        resposta.json().forEach(cliente -> ids.add(cliente.path("id").asLong()));
        confirmar(ids.contains(id), "O cliente " + id + " não aparece em GET /clientes");
    }

    @Step("Cadastrar uma cota de crédito <valor> para esse cliente")
    public void cadastrarCota(String valor) {
        Api.Resposta resposta = Api.post("/cotas", (String) ScenarioDataStore.get("token"), Map.of(
                "numero", "SMOKE-" + unico(), "valorCredito", new BigDecimal(valor),
                "clienteId", ScenarioDataStore.get("clienteId")));
        conferirStatus("POST /cotas", resposta, "201");
        ScenarioDataStore.put("cotaId", resposta.json().path("id").asLong());
    }

    @Step("A cota cadastrada mostra crédito disponível de <valor>")
    public void creditoDisponivel(String valor) {
        Api.Resposta resposta = Api.get("/cotas/" + ScenarioDataStore.get("cotaId"), (String) ScenarioDataStore.get("token"));
        conferirStatus("GET /cotas/{id}", resposta, "200");
        BigDecimal disponivel = resposta.json().path("creditoDisponivel").decimalValue();
        confirmar(disponivel.compareTo(new BigDecimal(valor)) == 0,
                "Crédito disponível esperado " + valor + ", obtido " + disponivel);
    }

    @Step("Com o papel <papel>, as consultas respondem <tabela>")
    public void consultasRespondem(String papel, Table tabela) {
        String token = Token.para("smoke-" + papel, papel);
        List<String> falhas = new ArrayList<>();
        for (TableRow linha : tabela.getTableRows()) {
            String caminho = linha.getCell("caminho");
            Api.Resposta resposta = Api.get(caminho, token);
            if (resposta.status() != Integer.parseInt(linha.getCell("status"))) {
                falhas.add("GET " + caminho + ": esperado " + linha.getCell("status") + ", obtido " + resposta.status());
            }
        }
        confirmar(falhas.isEmpty(), String.join("\n", falhas));
    }

    private static void conferirStatus(String chamada, Api.Resposta resposta, String esperado) {
        confirmar(resposta.status() == Integer.parseInt(esperado),
                chamada + ": esperado " + esperado + ", obtido " + resposta.status() + " " + resposta.corpo());
    }

    private static void confirmar(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new AssertionError(mensagem);
        }
    }

    /** Valor único por execução (CPF e número de cota não se repetem no banco, que persiste entre execuções). */
    private static String unico() {
        return String.valueOf(ThreadLocalRandom.current().nextLong(10_000_000_000L, 99_999_999_999L));
    }
}
