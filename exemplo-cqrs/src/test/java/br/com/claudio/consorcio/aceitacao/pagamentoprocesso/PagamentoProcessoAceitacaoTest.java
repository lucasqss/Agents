package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.abrirProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.enviar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.novaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.quantidadeDeProcessosDaCota;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ABERTO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.APROVADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.IMOBILIARIO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.SERVICO;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.claudio.consorcio.aceitacao.TesourariaStub;
import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AcaoInvalida;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Caminho;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Passo;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Padrão: Teste de aceitação baseado em modelos [Ammann e Offutt; Myers].
 * Roda na fase unitária e, por herança em {@link PagamentoProcessoAceitacaoIT}, contra o artefato empacotado.
 */
@QuarkusTest
@WithTestResource(TesourariaStubResource.class)
class PagamentoProcessoAceitacaoTest {

    private static final long INEXISTENTE = 999_999_999L;

    @BeforeEach
    void tesourariaNoAr() {
        TesourariaStub.ficarDisponivel();
    }

    // Grafo: todos os caminhos até uma situação final (regras 2, 3 e 4).

    static Stream<Caminho> caminhosCompletos() {
        return ModeloPagamentoProcesso.caminhosCompletos();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosCompletos")
    @DisplayName("Fluxo de negócio chega à situação final")
    void fluxoChegaASituacaoFinal(Caminho caminho) {
        long processo = abrirProcesso(novaCota(), caminho.tipo());
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));

        for (Passo passo : caminho.passos()) {
            executar(processo, passo.acao()).statusCode(200).body("estado", equalTo(passo.situacao()));
            assertThat(consultarProcesso(processo).get("estado"), equalTo(passo.situacao()));
        }
    }

    // Grafo: ações fora da ordem em cada situação (regra 5).

    static Stream<AcaoInvalida> acoesInvalidas() {
        return ModeloPagamentoProcesso.acoesInvalidas();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acoesInvalidas")
    @DisplayName("Ação fora da ordem é recusada e a situação não muda")
    void acaoForaDaOrdemERecusada(AcaoInvalida invalida) {
        long processo = abrirProcesso(novaCota(), invalida.ate().tipo());
        invalida.ate().passos().forEach(p -> executar(processo, p.acao()).statusCode(200));

        executar(processo, invalida.acao()).statusCode(409).body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(invalida.ate().situacaoFinal()));
    }

    // Particionamento do espaço de entrada de POST /processos, com Base Choice (regra 1).

    @Test
    @DisplayName("Escolha-base: cota existente e tipo serviço abre o processo")
    void escolhaBaseAbreProcesso() {
        long cota = novaCota();

        long processo = enviar("/processos", Map.of("cotaId", cota, "tipo", SERVICO)).statusCode(201)
                .extract().jsonPath().getLong("id");

        Map<String, Object> registrado = consultarProcesso(processo);
        assertThat(((Number) registrado.get("cotaId")).longValue(), equalTo(cota));
        assertThat(registrado.get("tipo"), equalTo(SERVICO));
        assertThat(registrado.get("estado"), equalTo(ABERTO));
        assertThat(registrado.get("protocoloPagamento"), nullValue());
    }

    @Test
    @DisplayName("Partição do tipo: imobiliário abre o processo")
    void tipoImobiliarioAbreProcesso() {
        long processo = enviar("/processos", Map.of("cotaId", novaCota(), "tipo", IMOBILIARIO)).statusCode(201)
                .extract().jsonPath().getLong("id");

        assertThat(consultarProcesso(processo).get("tipo"), equalTo(IMOBILIARIO));
    }

    @Test
    @DisplayName("Partição da cota: cota inexistente é recusada com 404")
    void cotaInexistente() {
        enviar("/processos", Map.of("cotaId", INEXISTENTE, "tipo", SERVICO)).statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Partição da cota: sem cota é recusado com 400 indicando o campo")
    void cotaAusente() {
        enviar("/processos", Map.of("tipo", SERVICO)).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("cotaId"));
    }

    @Test
    @DisplayName("Partição do tipo: tipo fora da lista é recusado com 400")
    void tipoInvalido() {
        enviar("/processos", Map.of("cotaId", novaCota(), "tipo", "CONSTRUCAO")).statusCode(400)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Partição do tipo: sem tipo é recusado com 400 indicando o campo")
    void tipoAusente() {
        enviar("/processos", Map.of("cotaId", novaCota())).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("tipo"));
    }

    // Tabela de decisão: no máximo um processo em andamento por cota (regra 6).

    static Stream<Caminho> caminhosEmAndamento() {
        return ModeloPagamentoProcesso.caminhosEmAndamento();
    }

    static Stream<Caminho> caminhosFinalizados() {
        return ModeloPagamentoProcesso.caminhosFinalizados();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosEmAndamento")
    @DisplayName("Cota com processo em andamento não abre outro")
    void cotaComProcessoEmAndamentoNaoAbreOutro(Caminho caminho) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, caminho.tipo());
        caminho.passos().forEach(p -> executar(processo, p.acao()).statusCode(200));

        enviar("/processos", Map.of("cotaId", cota, "tipo", SERVICO)).statusCode(409)
                .body("mensagem", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosFinalizados")
    @DisplayName("Cota com processo finalizado pode abrir outro")
    void cotaComProcessoFinalizadoAbreOutro(Caminho caminho) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, caminho.tipo());
        caminho.passos().forEach(p -> executar(processo, p.acao()).statusCode(200));

        long novo = abrirProcesso(cota, SERVICO);

        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
    }

    @Test
    @DisplayName("Processo em andamento de outra cota não impede abrir")
    void processoDeOutraCotaNaoImpede() {
        abrirProcesso(novaCota(), SERVICO);

        long processo = abrirProcesso(novaCota(), SERVICO);

        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));
    }

    @Test
    @DisplayName("Pedidos simultâneos na mesma cota abrem um único processo")
    void pedidosSimultaneosAbremUmUnicoProcesso() throws Exception {
        long cota = novaCota();
        int quantidade = 8;
        CyclicBarrier largada = new CyclicBarrier(quantidade);
        Callable<Integer> pedido = () -> {
            largada.await(30, TimeUnit.SECONDS);
            return enviar("/processos", Map.of("cotaId", cota, "tipo", SERVICO)).extract().statusCode();
        };

        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
            for (Future<Integer> resposta : executor.invokeAll(Collections.nCopies(quantidade, pedido))) {
                respostas.add(resposta.get());
            }
        }

        assertThat(Collections.frequency(respostas, 201), equalTo(1));
        assertThat(Collections.frequency(respostas, 409), equalTo(quantidade - 1));
        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
    }

    // Tesouraria (regras 7 e 8).

    @Test
    @DisplayName("Pagamento guarda o protocolo emitido pela tesouraria")
    void pagamentoGuardaProtocolo() {
        long processo = processoAprovado();
        assertThat(consultarProcesso(processo).get("protocoloPagamento"), nullValue());

        executar(processo, PAGAR).statusCode(200).body("estado", equalTo(PAGO));

        assertThat(consultarProcesso(processo).get("protocoloPagamento"), equalTo(TesourariaStub.PROTOCOLO));
    }

    @Test
    @DisplayName("Tesouraria indisponível recusa o pagamento, e ele pode ser refeito depois")
    void tesourariaIndisponivel() {
        long processo = processoAprovado();

        TesourariaStub.ficarIndisponivel();
        executar(processo, PAGAR).statusCode(503).body("mensagem", notNullValue());

        Map<String, Object> aposFalha = consultarProcesso(processo);
        assertThat(aposFalha.get("estado"), equalTo(APROVADO));
        assertThat(aposFalha.get("protocoloPagamento"), nullValue());

        TesourariaStub.ficarDisponivel();
        executar(processo, PAGAR).statusCode(200).body("estado", equalTo(PAGO));
        assertThat(consultarProcesso(processo).get("protocoloPagamento"), equalTo(TesourariaStub.PROTOCOLO));
    }

    // Terminais do contrato que os grupos acima não alcançam.

    static Stream<String> acoes() {
        return ModeloPagamentoProcesso.acoes();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("acoes")
    @DisplayName("Ação em processo inexistente é recusada com 404")
    void acaoEmProcessoInexistente(String acao) {
        executar(INEXISTENTE, acao).statusCode(404).body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Listagem filtra por tipo e situação")
    void listagemFiltraPorTipoESituacao() {
        long processo = abrirProcesso(novaCota(), IMOBILIARIO);
        executar(processo, INICIAR_ANALISE).statusCode(200);

        given().queryParam("tipo", IMOBILIARIO).queryParam("estado", "EM_ANALISE").queryParam("tamanho", 100)
                .when().get("/processos").then().statusCode(200)
                .body("itens.tipo", everyItem(equalTo(IMOBILIARIO)))
                .body("itens.estado", everyItem(equalTo("EM_ANALISE")));
    }

    @Test
    @DisplayName("Listagem respeita página e tamanho e informa o total")
    void listagemPaginada() {
        abrirProcesso(novaCota(), SERVICO);

        given().queryParam("pagina", 1).queryParam("tamanho", 1).when().get("/processos").then().statusCode(200)
                .body("pagina", equalTo(1))
                .body("tamanho", equalTo(1))
                .body("itens.size()", lessThanOrEqualTo(1))
                .body("total", notNullValue());
    }

    // Suposição de erro.

    @Test
    @DisplayName("Corpo malformado é recusado com 400")
    void corpoMalformado() {
        given().contentType(ContentType.JSON).body("{\"cotaId\": ").when().post("/processos").then().statusCode(400)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Corpo vazio é recusado com 400")
    void corpoVazio() {
        given().contentType(ContentType.JSON).when().post("/processos").then().statusCode(400)
                .body("mensagem", notNullValue());
    }

    private static long processoAprovado() {
        long processo = abrirProcesso(novaCota(), SERVICO);
        executar(processo, INICIAR_ANALISE).statusCode(200);
        executar(processo, CONCLUIR_ANALISE).statusCode(200).body("estado", equalTo(APROVADO));
        return processo;
    }
}
