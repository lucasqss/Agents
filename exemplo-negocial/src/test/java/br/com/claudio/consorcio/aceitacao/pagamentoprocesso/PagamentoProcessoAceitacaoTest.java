package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.AUSENTE;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.CREDITO_PADRAO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.VALOR_PADRAO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.abrirProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarCreditoDisponivel;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarFavorecidos;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.corpoAbertura;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarProposta;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.favorecidoUnico;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.json;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarAlterar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarConsultarProposta;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarProporAlteracao;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarConsultarFavorecidos;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarAbrirProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.valorDoProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.enviar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarAcoes;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarHistorico;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComConteudo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComCorpo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComMotivo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComObservacao;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.listarProcessosPorSituacao;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.novaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.quantidadeDeProcessosDaCota;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ABERTO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AGUARDANDO_CONCORDANCIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ALTERAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CONCLUIR_VISTORIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CONCORDAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.DISCORDAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PROPOR_ALTERACAO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PENDENTE_DOCUMENTO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.REPROVADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.REPROVAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.EM_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.caminhoAte;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.exigeMotivo;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.motivoPadrao;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.nomeNoHistorico;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.APROVADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CONCLUIR_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.IMOBILIARIO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CANCELADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CANCELAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.SERVICO;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.claudio.consorcio.aceitacao.ApiAceitacao.Proposta;
import br.com.claudio.consorcio.aceitacao.TesourariaStub;
import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AcaoInvalida;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Alteracao;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ProcessoEsperado;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Registro;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Base;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Caminho;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CasoFavorecidos;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CenarioCredito;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ProcessoPlanejado;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Passo;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Resultado;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.time.Instant;
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
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ValueSource;
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

    static Stream<Caminho> caminhosCompletosComAlteracao() {
        return ModeloPagamentoProcesso.caminhosCompletosComAlteracao();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosCompletos")
    @DisplayName("Fluxo de negócio chega à situação final")
    void fluxoChegaASituacaoFinal(Caminho caminho) {
        percorrerEConferir(caminho);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosCompletosComAlteracao")
    @DisplayName("Fluxo com alteração, proposta e resposta do cliente chega à situação final, com os dados vigentes de cada passo")
    void fluxoComAlteracaoChegaASituacaoFinal(Caminho caminho) {
        percorrerEConferir(caminho);
    }

    private static void percorrerEConferir(Caminho caminho) {
        long cota = novaCota();
        ProcessoEsperado esperado = ProcessoEsperado.inicial();
        long processo = abrirProcesso(cota, caminho.tipo(), esperado.valor(), esperado.favorecidos());
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));

        for (Passo passo : caminho.passos()) {
            aplicar(processo, passo.acao(), esperado).statusCode(200).body("estado", equalTo(passo.situacao()));
            esperado.confirmar(passo.acao());
            // R24 a R28: a cada passo, a situação, o valor, os favorecidos, o crédito e a proposta pendente
            // são os vigentes esperados.
            conferirVigentes(cota, processo, esperado, passo.situacao());
        }
        conferirHistoricoDeValores(processo, esperado);

        // R19: cada ação aceita está no histórico, em ordem, com o motivo.
        List<Map<String, Object>> historico = consultarHistorico(processo);
        assertThat(historico.size(), equalTo(caminho.passos().size()));
        String anterior = ABERTO;
        Instant ultimo = Instant.MIN;
        for (int i = 0; i < historico.size(); i++) {
            Passo passo = caminho.passos().get(i);
            Map<String, Object> item = historico.get(i);
            assertThat(item.get("de"), equalTo(anterior));
            assertThat(item.get("para"), equalTo(passo.situacao()));
            assertThat(item.get("acao"), equalTo(nomeNoHistorico(passo.acao())));
            if (exigeMotivo(passo.acao())) {
                assertThat(item.get("observacao"), equalTo(motivoPadrao(passo.acao())));
            }
            Instant quando = Instant.parse((String) item.get("quando"));
            assertThat(quando, greaterThanOrEqualTo(ultimo));
            ultimo = quando;
            anterior = passo.situacao();
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
        invalida.ate().passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));
        int itensNoHistorico = consultarHistorico(processo).size();

        // Motivo válido nas ações que o exigem, para que só a ordem seja o problema.
        aplicar(processo, invalida.acao()).statusCode(409).body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(invalida.ate().situacaoFinal()));
        assertThat(consultarHistorico(processo).size(), equalTo(itensNoHistorico));
    }

    // Particionamento do espaço de entrada de POST /processos, com Base Choice (regra 1).

    @Test
    @DisplayName("Escolha-base: cota existente e tipo serviço abre o processo")
    void escolhaBaseAbreProcesso() {
        long cota = novaCota();

        long processo = tentarAbrirProcesso(cota, SERVICO).statusCode(201)
                .extract().jsonPath().getLong("id");

        Map<String, Object> registrado = consultarProcesso(processo);
        assertThat(((Number) registrado.get("cotaId")).longValue(), equalTo(cota));
        assertThat(registrado.get("tipo"), equalTo(SERVICO));
        assertThat(registrado.get("estado"), equalTo(ABERTO));
        assertThat(registrado.get("protocoloPagamento"), nullValue());
        assertThat(valorDoProcesso(registrado), comparesEqualTo(VALOR_PADRAO));
    }

    @Test
    @DisplayName("Partição do tipo: imobiliário abre o processo")
    void tipoImobiliarioAbreProcesso() {
        long processo = tentarAbrirProcesso(novaCota(), IMOBILIARIO).statusCode(201)
                .extract().jsonPath().getLong("id");

        assertThat(consultarProcesso(processo).get("tipo"), equalTo(IMOBILIARIO));
    }

    @Test
    @DisplayName("Partição da cota: cota inexistente é recusada com 404")
    void cotaInexistente() {
        tentarAbrirProcesso(INEXISTENTE, SERVICO).statusCode(404)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Partição da cota: sem cota é recusado com 400 indicando o campo")
    void cotaAusente() {
        enviar("/processos", corpoAbertura(AUSENTE, SERVICO, VALOR_PADRAO, favorecidoUnico(VALOR_PADRAO)))
                .statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("cotaId"));
    }

    @Test
    @DisplayName("Partição do tipo: tipo fora da lista é recusado com 400")
    void tipoInvalido() {
        enviar("/processos", corpoAbertura(novaCota(), "CONSTRUCAO", VALOR_PADRAO, favorecidoUnico(VALOR_PADRAO)))
                .statusCode(400)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Partição do tipo: sem tipo é recusado com 400 indicando o campo")
    void tipoAusente() {
        enviar("/processos", corpoAbertura(novaCota(), AUSENTE, VALOR_PADRAO, favorecidoUnico(VALOR_PADRAO)))
                .statusCode(400)
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
        caminho.passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));

        tentarAbrirProcesso(cota, SERVICO).statusCode(409)
                .body("mensagem", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosFinalizados")
    @DisplayName("Cota com processo finalizado pode abrir outro")
    void cotaComProcessoFinalizadoAbreOutro(Caminho caminho) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, caminho.tipo());
        caminho.passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));

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
            return tentarAbrirProcesso(cota, SERVICO).extract().statusCode();
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
        aplicar(INEXISTENTE, acao).statusCode(404).body("mensagem", notNullValue());
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


    // R6 (fatia 1): reprovado libera a cota, como cancelado e pago.

    @Test
    @DisplayName("Cota com processo reprovado abre outro, e o reprovado continua final")
    void reprovadoLiberaACota() {
        long cota = novaCota();
        long reprovado = abrirProcesso(cota, SERVICO);
        executar(reprovado, INICIAR_ANALISE).statusCode(200);
        executarComMotivo(reprovado, REPROVAR, "favorecido divergente").statusCode(200);

        long novo = abrirProcesso(cota, SERVICO);

        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
        assertThat(consultarProcesso(reprovado).get("estado"), equalTo(REPROVADO));
        executar(novo, INICIAR_ANALISE).statusCode(200).body("estado", equalTo(EM_ANALISE));
    }

    // Máquina de estados: as ações aceitas em cada situação (consulta /acoes).

    static Stream<Caminho> caminhosPorSituacao() {
        return ModeloPagamentoProcesso.caminhosPorSituacao();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosPorSituacao")
    @DisplayName("Consulta de ações lista exatamente as ações aceitas na situação")
    void acoesAceitasNaSituacao(Caminho caminho) {
        long processo = processoEm(caminho);

        List<String> acoes = consultarAcoes(processo);

        assertThat(acoes, containsInAnyOrder(
                ModeloPagamentoProcesso.acoesAceitas(caminho.tipo(), caminho.situacaoFinal()).toArray()));
    }

    // Particionamento do motivo (R4, R10, R11, R23): ausente, vazio, só espaços, 1, 500 e 501 caracteres.

    static Stream<Base> transicoesComMotivo() {
        return ModeloPagamentoProcesso.transicoesComMotivo();
    }

    static Stream<Base> umaTransicaoPorAcaoComMotivo() {
        return ModeloPagamentoProcesso.umaTransicaoPorAcaoComMotivo();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transicoesComMotivo")
    @DisplayName("Motivo ausente (objeto sem o campo) é recusado com 400 e nada muda")
    void motivoAusenteERecusado(Base base) {
        long processo = processoEm(base);
        int itensNoHistorico = consultarHistorico(processo).size();

        aplicarComMotivo(processo, base.acao(), AUSENTE).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("motivo"));

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.situacao()));
        assertThat(consultarHistorico(processo).size(), equalTo(itensNoHistorico));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("umaTransicaoPorAcaoComMotivo")
    @DisplayName("Corpo ausente em ação que exige motivo é recusado com 400 e nada muda")
    void corpoAusenteEmAcaoComMotivo(Base base) {
        long processo = processoEm(base);
        int itensNoHistorico = consultarHistorico(processo).size();

        executar(processo, base.acao()).statusCode(400).body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.situacao()));
        assertThat(consultarHistorico(processo).size(), equalTo(itensNoHistorico));
    }

    static Stream<Arguments> motivosInvalidos() {
        return ModeloPagamentoProcesso.umaTransicaoPorAcaoComMotivo().flatMap(base -> Stream.of(
                Arguments.of(base, "motivo vazio", ""),
                Arguments.of(base, "motivo só com espaços", "   \t "),
                Arguments.of(base, "motivo com 501 caracteres", "a".repeat(501))));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("motivosInvalidos")
    @DisplayName("Motivo vazio, só espaços ou acima de 500 caracteres é recusado com 400 e nada muda")
    void motivoInvalidoERecusado(Base base, String descricao, String motivo) {
        long processo = processoEm(base);
        int itensNoHistorico = consultarHistorico(processo).size();

        aplicarComMotivo(processo, base.acao(), motivo).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("motivo"));

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.situacao()));
        assertThat(consultarHistorico(processo).size(), equalTo(itensNoHistorico));
    }

    static Stream<Arguments> motivosValidos() {
        return ModeloPagamentoProcesso.umaTransicaoPorAcaoComMotivo().flatMap(base -> Stream.of(
                Arguments.of(base, "motivo com 1 caractere", "a"),
                Arguments.of(base, "motivo com 500 caracteres", "a".repeat(500)),
                Arguments.of(base, "motivo com 500 caracteres acentuados", "ã".repeat(500))));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("motivosValidos")
    @DisplayName("Motivo de 1 a 500 caracteres é aceito e aparece no histórico")
    void motivoValidoFicaNoHistorico(Base base, String descricao, String motivo) {
        long processo = processoEm(base);

        aplicarComMotivo(processo, base.acao(), motivo).statusCode(200).body("estado", equalTo(base.destino()));

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.destino()));
        Map<String, Object> ultimo = consultarHistorico(processo).getLast();
        assertThat(ultimo.get("de"), equalTo(base.situacao()));
        assertThat(ultimo.get("para"), equalTo(base.destino()));
        assertThat(ultimo.get("acao"), equalTo(nomeNoHistorico(base.acao())));
        assertThat(ultimo.get("observacao"), equalTo(motivo));
    }

    // Observação opcional (iniciar análise, concluir análise, concluir vistoria, receber documento).

    static Stream<Base> transicoesComObservacao() {
        return ModeloPagamentoProcesso.transicoesComObservacao();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("transicoesComObservacao")
    @DisplayName("Ação de observação opcional é aceita com objeto vazio")
    void observacaoOpcionalAceitaObjetoVazio(Base base) {
        long processo = processoEm(base);

        executarComCorpo(processo, base.acao(), "{}").statusCode(200).body("estado", equalTo(base.destino()));

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.destino()));
    }

    static Stream<Arguments> observacoesValidas() {
        return ModeloPagamentoProcesso.transicoesComObservacao().flatMap(base -> Stream.of(
                Arguments.of(base, "observação curta", "recebido pelo balcão"),
                Arguments.of(base, "observação com 500 caracteres", "a".repeat(500))));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("observacoesValidas")
    @DisplayName("Observação informada é aceita e aparece no histórico")
    void observacaoFicaNoHistorico(Base base, String descricao, String observacao) {
        long processo = processoEm(base);

        executarComObservacao(processo, base.acao(), observacao).statusCode(200)
                .body("estado", equalTo(base.destino()));

        Map<String, Object> ultimo = consultarHistorico(processo).getLast();
        assertThat(ultimo.get("para"), equalTo(base.destino()));
        assertThat(ultimo.get("acao"), equalTo(nomeNoHistorico(base.acao())));
        assertThat(ultimo.get("observacao"), equalTo(observacao));
    }

    // Suposição de erro nas ações com corpo: JSON malformado (400) e tipo de conteúdo que não é JSON (415).

    static Stream<Base> umaTransicaoPorAcaoComCorpo() {
        return ModeloPagamentoProcesso.umaTransicaoPorAcaoComCorpo();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("umaTransicaoPorAcaoComCorpo")
    @DisplayName("JSON malformado em ação com corpo é recusado com 400 e nada muda")
    void jsonMalformadoNaAcao(Base base) {
        long processo = processoEm(base);

        executarComCorpo(processo, base.acao(), "{\"motivo\": ").statusCode(400)
                .body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.situacao()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("umaTransicaoPorAcaoComCorpo")
    @DisplayName("Corpo que não é JSON em ação com corpo é recusado com 415 e nada muda")
    void corpoQueNaoEJsonNaAcao(Base base) {
        long processo = processoEm(base);

        executarComConteudo(processo, base.acao(), "text/plain", "motivo=teste").statusCode(415)
                .body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(base.situacao()));
    }

    // Consulta: filtro por situação aceita as situações novas (fatia 1).

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {PENDENTE_DOCUMENTO, REPROVADO, AGUARDANDO_CONCORDANCIA})
    @DisplayName("Listagem filtra pelas situações pendente de documento, reprovado e aguardando concordância")
    void listagemFiltraPorSituacoesNovas(String situacao) {
        long processo = processoEm(caminhoAte(SERVICO, situacao));

        List<Map<String, Object>> itens = listarProcessosPorSituacao(situacao);

        assertThat(itens.stream().map(i -> ((Number) i.get("id")).longValue()).toList(), hasItem(processo));
        assertThat(itens.stream().map(i -> i.get("estado")).distinct().toList(), equalTo(List.of(situacao)));
    }

    // ---- Fatia 2: valor e crédito (R12, R13, R14) ----

    /** Pedido com um favorecido válido que recebe {@code parteJson} (R15 e R29: o favorecido não é o assunto). */
    private static String abertura(long cota, String tipo, String valorJson, String parteJson) {
        return ("{\"cotaId\": %d, \"tipo\": \"%s\"%s, \"favorecidos\": [{\"nome\": \"Favorecido de Aceitacao\", "
                + "\"documento\": \"12345678901\", \"papel\": \"vendedor\", \"valor\": %s}]}").formatted(cota, tipo,
                valorJson == null ? "" : ", \"valor\": " + valorJson, parteJson);
    }

    /** Valor válido: o favorecido recebe o valor inteiro. */
    private static String abertura(long cota, String tipo, String valorJson) {
        return abertura(cota, tipo, valorJson, valorJson);
    }

    /** Valor inválido ou ausente: o favorecido é válido e recebe 100,00, para o único defeito ser o valor. */
    private static String aberturaComValorInvalido(long cota, String tipo, String valorJson) {
        return abertura(cota, tipo, valorJson, "100.00");
    }

    // Particionamento do valor (R12): classes válidas e inválidas e valores-limite.

    static Stream<Arguments> valoresValidos() {
        return Stream.of(
                Arguments.of("limite inferior: 0,01", "0.01", new BigDecimal("0.01")),
                Arguments.of("duas casas decimais", "12500.50", new BigDecimal("12500.50")),
                Arguments.of("inteiro, sem casas", "100", new BigDecimal("100")),
                Arguments.of("uma casa decimal", "100.5", new BigDecimal("100.5")),
                Arguments.of("exatamente o crédito da cota", "250000.00", new BigDecimal("250000.00")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("valoresValidos")
    @DisplayName("Valor maior que zero com até duas casas abre o processo e aparece na listagem")
    void valorValidoAbreProcesso(String descricao, String valorJson, BigDecimal esperado) {
        long cota = novaCota();

        long processo = enviar("/processos", abertura(cota, SERVICO, valorJson)).statusCode(201)
                .extract().jsonPath().getLong("id");

        Map<String, Object> registrado = consultarProcesso(processo);
        assertThat(registrado.get("estado"), equalTo(ABERTO));
        assertThat(valorDoProcesso(registrado), comparesEqualTo(esperado));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
    }

    static Stream<Arguments> valoresInvalidos() {
        return Stream.of(
                Arguments.of("ausente", null),
                Arguments.of("nulo", "null"),
                Arguments.of("zero", "0"),
                Arguments.of("zero com duas casas", "0.00"),
                Arguments.of("negativo: -0,01", "-0.01"),
                Arguments.of("negativo: -100", "-100"),
                Arguments.of("três casas: 100,005", "100.005"),
                Arguments.of("três casas: 0,001", "0.001"),
                Arguments.of("três casas: 100,123", "100.123"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("valoresInvalidos")
    @DisplayName("Valor ausente, nulo, zero, negativo ou com mais de duas casas é recusado com 400 e nada é criado")
    void valorInvalidoERecusado(String descricao, String valorJson) {
        long cota = novaCota();

        enviar("/processos", aberturaComValorInvalido(cota, SERVICO, valorJson)).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("valor"));

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(0L));
        // A cota continua livre: outro pedido, com valor válido, abre.
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("0.01"));
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SERVICO, IMOBILIARIO})
    @DisplayName("Valor ausente é recusado nos dois tipos")
    void valorAusenteNosDoisTipos(String tipo) {
        enviar("/processos", aberturaComValorInvalido(novaCota(), tipo, null)).statusCode(400)
                .body("campos.campo", hasItem("valor"));
    }

    @Test
    @DisplayName("Valor inválido em cota inexistente: o 400 vem antes do 404")
    void valorInvalidoVemAntesDeCotaInexistente() {
        enviar("/processos", aberturaComValorInvalido(INEXISTENTE, SERVICO, "0")).statusCode(400)
                .body("campos.campo", hasItem("valor"));
    }

    @Test
    @DisplayName("Valor ausente em cota inexistente: o 400 vem antes do 404")
    void valorAusenteVemAntesDeCotaInexistente() {
        enviar("/processos", aberturaComValorInvalido(INEXISTENTE, SERVICO, null)).statusCode(400)
                .body("campos.campo", hasItem("valor"));
    }

    @Test
    @DisplayName("Valor inválido em cota com processo em andamento: o 400 vem antes do 409")
    void valorInvalidoVemAntesDeCotaIndisponivel() {
        long cota = novaCota();
        abrirProcesso(cota, SERVICO);

        enviar("/processos", aberturaComValorInvalido(cota, SERVICO, "100.005")).statusCode(400)
                .body("campos.campo", hasItem("valor"));

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
    }

    // Modelo de crédito (R13, R14): oráculo calculado em ModeloPagamentoProcesso.

    static Stream<CenarioCredito> cenariosDeCredito() {
        return ModeloPagamentoProcesso.cenariosDeCredito();
    }

    static Stream<CenarioCredito> cenariosComCotaLivre() {
        return ModeloPagamentoProcesso.cenariosComCotaLivre();
    }

    static Stream<CenarioCredito> cenariosComCotaLivreECreditoRestante() {
        return ModeloPagamentoProcesso.cenariosComCotaLivre()
                .filter(c -> c.creditoDisponivelEsperado().signum() > 0);
    }

    /** Cota nova com o crédito do cenário e seus processos levados às situações do cenário. */
    private static long cotaDoCenario(CenarioCredito cenario) {
        long cota = novaCota(cenario.credito());
        for (ProcessoPlanejado planejado : cenario.processos()) {
            long processo = abrirProcesso(cota, planejado.tipo(), planejado.valor());
            planejado.caminho().passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));
        }
        return cota;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenariosDeCredito")
    @DisplayName("Crédito disponível é o crédito da cota menos o valor dos pagos")
    void creditoDisponivelConsideraSoOsPagos(CenarioCredito cenario) {
        long cota = cotaDoCenario(cenario);

        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(cenario.creditoDisponivelEsperado()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenariosComCotaLivreECreditoRestante")
    @DisplayName("Valor exatamente igual ao crédito disponível é aceito e não consome crédito enquanto não é pago")
    void valorIgualAoCreditoDisponivelEAceito(CenarioCredito cenario) {
        long cota = cotaDoCenario(cenario);
        BigDecimal disponivel = cenario.creditoDisponivelEsperado();

        long processo = abrirProcesso(cota, SERVICO, disponivel);

        Map<String, Object> registrado = consultarProcesso(processo);
        assertThat(registrado.get("estado"), equalTo(ABERTO));
        assertThat(valorDoProcesso(registrado), comparesEqualTo(disponivel));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(disponivel));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("cenariosComCotaLivre")
    @DisplayName("Valor um centavo acima do crédito disponível é recusado com 409 e nada é criado")
    void valorUmCentavoAcimaDoCreditoERecusado(CenarioCredito cenario) {
        long cota = cotaDoCenario(cenario);
        BigDecimal disponivel = cenario.creditoDisponivelEsperado();

        tentarAbrirProcesso(cota, SERVICO, disponivel.add(new BigDecimal("0.01"))).statusCode(409)
                .body("mensagem", notNullValue());

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo((long) cenario.processos().size()));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(disponivel));
        if (disponivel.signum() > 0) {
            // A recusa não deixou a cota indisponível: outro pedido, dentro do crédito, abre.
            long processo = abrirProcesso(cota, SERVICO, new BigDecimal("0.01"));
            assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));
        }
    }

    @Test
    @DisplayName("Cota de 250.000 com 100.000 pagos: 150.000,00 é aceito e 150.000,01 é recusado")
    void exemploDoCasoDeUsoCreditoRestante() {
        long cota = novaCota(new BigDecimal("250000.00"));
        long pago = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        aplicar(pago, INICIAR_ANALISE).statusCode(200);
        aplicar(pago, CONCLUIR_ANALISE).statusCode(200);
        aplicar(pago, PAGAR).statusCode(200).body("estado", equalTo(PAGO));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(new BigDecimal("150000.00")));

        tentarAbrirProcesso(cota, SERVICO, new BigDecimal("150000.01")).statusCode(409);
        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));

        long novo = abrirProcesso(cota, SERVICO, new BigDecimal("150000.00"));
        assertThat(valorDoProcesso(consultarProcesso(novo)), comparesEqualTo(new BigDecimal("150000.00")));
    }

    @Test
    @DisplayName("Cota nova informa o crédito inteiro como disponível")
    void cotaNovaTemOCreditoInteiroDisponivel() {
        assertThat(consultarCreditoDisponivel(novaCota(new BigDecimal("1000.50"))),
                comparesEqualTo(new BigDecimal("1000.50")));
    }

    @Test
    @DisplayName("Crédito esgotado: qualquer valor válido é recusado")
    void creditoEsgotadoRecusaQualquerValor() {
        long cota = novaCota(new BigDecimal("1000.00"));
        long pago = abrirProcesso(cota, SERVICO, new BigDecimal("1000.00"));
        aplicar(pago, INICIAR_ANALISE).statusCode(200);
        aplicar(pago, CONCLUIR_ANALISE).statusCode(200);
        aplicar(pago, PAGAR).statusCode(200);

        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(BigDecimal.ZERO));
        tentarAbrirProcesso(cota, SERVICO, new BigDecimal("0.01")).statusCode(409);
        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
    }

    @Test
    @DisplayName("Pagamento de outra cota não reduz o crédito disponível desta")
    void processoDeOutraCotaNaoReduzCredito() {
        long cota = novaCota();
        long outra = novaCota();
        long pago = abrirProcesso(outra, SERVICO, new BigDecimal("100000.00"));
        aplicar(pago, INICIAR_ANALISE).statusCode(200);
        aplicar(pago, CONCLUIR_ANALISE).statusCode(200);
        aplicar(pago, PAGAR).statusCode(200);

        assertThat(consultarCreditoDisponivel(outra), comparesEqualTo(new BigDecimal("150000.00")));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
        long processo = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);
        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(CREDITO_PADRAO));
    }

    @Test
    @DisplayName("Pagamento recusado pela tesouraria (503) não consome crédito")
    void pagamentoNaoConcluidoNaoConsomeCredito() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        aplicar(processo, INICIAR_ANALISE).statusCode(200);
        aplicar(processo, CONCLUIR_ANALISE).statusCode(200);

        TesourariaStub.ficarIndisponivel();
        aplicar(processo, PAGAR).statusCode(503);

        assertThat(consultarProcesso(processo).get("estado"), equalTo(APROVADO));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));

        TesourariaStub.ficarDisponivel();
        aplicar(processo, PAGAR).statusCode(200);
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(new BigDecimal("150000.00")));
    }

    @Test
    @DisplayName("Crédito só diminui quando o processo é pago: aprovado ainda não consome")
    void creditoSoDiminuiNoPagamento() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        aplicar(processo, INICIAR_ANALISE).statusCode(200);
        aplicar(processo, CONCLUIR_ANALISE).statusCode(200);
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));

        aplicar(processo, PAGAR).statusCode(200);

        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(new BigDecimal("150000.00")));
    }

    @Test
    @DisplayName("R14: cancelar um processo que consumiria todo o crédito libera a cota e o crédito")
    void cancelarLiberaCotaECredito() {
        long cota = novaCota();
        long cancelado = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);
        aplicar(cancelado, CANCELAR).statusCode(200).body("estado", equalTo(CANCELADO));

        long novo = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);

        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
        assertThat(consultarProcesso(cancelado).get("estado"), equalTo(CANCELADO));
        assertThat(valorDoProcesso(consultarProcesso(cancelado)), comparesEqualTo(CREDITO_PADRAO));
    }

    @Test
    @DisplayName("R14: reprovar um processo que consumiria todo o crédito libera a cota e o crédito")
    void reprovarLiberaCotaECredito() {
        long cota = novaCota();
        long reprovado = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);
        aplicar(reprovado, INICIAR_ANALISE).statusCode(200);
        aplicar(reprovado, REPROVAR).statusCode(200).body("estado", equalTo(REPROVADO));

        long novo = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);

        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
    }

    // Consulta: a listagem mostra o valor vigente de cada processo.

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SERVICO, IMOBILIARIO})
    @DisplayName("Listagem mostra o valor de cada processo, inclusive depois de pago")
    void listagemMostraOValor(String tipo) {
        long cota = novaCota();
        BigDecimal valor = new BigDecimal("12500.50");
        long processo = abrirProcesso(cota, tipo, valor);
        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(valor));

        caminhoAte(tipo, APROVADO).passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));
        aplicar(processo, PAGAR).statusCode(200);

        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(valor));
    }

    @Test
    @DisplayName("Listagem mostra valores diferentes em processos diferentes, sem confundir um com o outro")
    void listagemMostraOValorDeCadaProcesso() {
        long primeiro = abrirProcesso(novaCota(), SERVICO, new BigDecimal("0.01"));
        long segundo = abrirProcesso(novaCota(), IMOBILIARIO, new BigDecimal("99999.99"));

        assertThat(valorDoProcesso(consultarProcesso(primeiro)), comparesEqualTo(new BigDecimal("0.01")));
        assertThat(valorDoProcesso(consultarProcesso(segundo)), comparesEqualTo(new BigDecimal("99999.99")));
    }

    // ---- Fatia 3: favorecidos na abertura (R15, R29) ----
    // Oráculo: ModeloPagamentoProcesso.CasoFavorecidos#resultado (aceito, recusa de formato ou recusa de regra).

    static Stream<CasoFavorecidos> favorecidosAceitos() {
        return ModeloPagamentoProcesso.favorecidosAceitos();
    }

    static Stream<CasoFavorecidos> favorecidosRecusadosPorFormato() {
        return ModeloPagamentoProcesso.favorecidosRecusadosPorFormato();
    }

    static Stream<CasoFavorecidos> favorecidosRecusadosPorRegra() {
        return ModeloPagamentoProcesso.favorecidosRecusadosPorRegra();
    }

    static Stream<CasoFavorecidos> tabelaDeDecisao() {
        return ModeloPagamentoProcesso.tabelaDeDecisao();
    }

    static Stream<CasoFavorecidos> favorecidosRecusadosPorFormatoRepresentativos() {
        return ModeloPagamentoProcesso.favorecidosRecusadosPorFormatoRepresentativos();
    }

    /** Abre com o caso (que o modelo manda aceitar) e confere o efeito: situação, valor, crédito e consulta. */
    private static long abrirEConferirAceito(CasoFavorecidos caso, String tipo) {
        assertThat("o modelo manda aceitar", caso.resultado(), equalTo(Resultado.ACEITO));
        long cota = novaCota();

        long processo = tentarAbrirProcesso(cota, tipo, caso.valor(), caso.favorecidos()).statusCode(201)
                .extract().jsonPath().getLong("id");

        Map<String, Object> registrado = consultarProcesso(processo);
        assertThat(registrado.get("estado"), equalTo(ABERTO));
        assertThat(valorDoProcesso(registrado), comparesEqualTo(caso.valor()));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
        conferirFavorecidos(processo, caso.esperados());
        BigDecimal soma = consultarFavorecidos(processo).stream().map(f -> parte(f)).reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(soma, comparesEqualTo(caso.valor()));
        return processo;
    }

    /** A consulta devolve exatamente os favorecidos informados, na mesma ordem, e as partes somam o valor. */
    private static void conferirFavorecidos(long processo, List<Map<String, Object>> esperados) {
        compararFavorecidos(consultarFavorecidos(processo), esperados);
    }

    private static void compararFavorecidos(List<Map<String, Object>> obtidos, List<Map<String, Object>> esperados) {
        assertThat(obtidos.size(), equalTo(esperados.size()));
        for (int i = 0; i < esperados.size(); i++) {
            Map<String, Object> esperado = esperados.get(i);
            Map<String, Object> obtido = obtidos.get(i);
            assertThat(obtido.get("nome"), equalTo(esperado.get("nome")));
            assertThat(obtido.get("documento"), equalTo(esperado.get("documento")));
            assertThat(obtido.get("papel"), equalTo(esperado.get("papel")));
            assertThat(parte(obtido), comparesEqualTo((BigDecimal) esperado.get("valor")));
        }
    }

    /** Parte do favorecido na consulta, sem passar por ponto flutuante quando o JSON permite. */
    private static BigDecimal parte(Map<String, Object> favorecido) {
        return new BigDecimal(String.valueOf(favorecido.get("valor")));
    }

    /**
     * Abre com o caso (que o modelo manda recusar) e confere: 400 com mensagem (e com campos de favorecidos quando a
     * recusa é de formato), nada criado, crédito intacto e cota livre, com abertura válida logo em seguida.
     */
    private static void abrirEConferirRecusado(CasoFavorecidos caso, Resultado esperado, String tipo) {
        assertThat("o modelo manda recusar", caso.resultado(), equalTo(esperado));
        long cota = novaCota();

        ValidatableResponse resposta = tentarAbrirProcesso(cota, tipo, caso.valor(), caso.favorecidos())
                .statusCode(400).body("mensagem", notNullValue());
        if (esperado == Resultado.RECUSA_DE_FORMATO) {
            resposta.body("campos.campo", hasItem(containsString("favorecidos")));
        }

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(0L));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
        long processo = abrirProcesso(cota, tipo, caso.valor());
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));
        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
        List<Map<String, Object>> favorecidos = consultarFavorecidos(processo);
        assertThat(favorecidos.size(), equalTo(1));
        assertThat(parte(favorecidos.getFirst()), comparesEqualTo(caso.valor()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosAceitos")
    @DisplayName("Favorecidos válidos somando o valor abrem o processo e a consulta os devolve na ordem informada")
    void favorecidosValidosAbremProcesso(CasoFavorecidos caso) {
        abrirEConferirAceito(caso, SERVICO);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SERVICO, IMOBILIARIO})
    @DisplayName("Exemplo do caso de uso (vendedor 9.500,00 e cartório 500,00) é aceito nos dois tipos")
    void exemploDoCasoDeUsoNosDoisTipos(String tipo) {
        abrirEConferirAceito(ModeloPagamentoProcesso.exemploDoCasoDeUso(), tipo);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosRecusadosPorFormato")
    @DisplayName("Favorecidos com dado ausente, em branco, fora do tamanho ou do formato: 400 com campos, nada é criado")
    void favorecidosComFormatoInvalidoSaoRecusados(CasoFavorecidos caso) {
        abrirEConferirRecusado(caso, Resultado.RECUSA_DE_FORMATO, SERVICO);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosRecusadosPorRegra")
    @DisplayName("Soma das partes diferente do valor ou documento repetido: 400 com mensagem, nada é criado")
    void favorecidosQueFeremARegraSaoRecusados(CasoFavorecidos caso) {
        abrirEConferirRecusado(caso, Resultado.RECUSA_DE_REGRA, SERVICO);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tabelaDeDecisao")
    @DisplayName("Tabela de decisão de R15 e R29: cada condição, sozinha, decide a abertura")
    void tabelaDeDecisaoDosFavorecidos(CasoFavorecidos caso) {
        switch (caso.resultado()) {
            case ACEITO -> abrirEConferirAceito(caso, SERVICO);
            case RECUSA_DE_FORMATO -> abrirEConferirRecusado(caso, Resultado.RECUSA_DE_FORMATO, SERVICO);
            case RECUSA_DE_REGRA -> abrirEConferirRecusado(caso, Resultado.RECUSA_DE_REGRA, SERVICO);
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {SERVICO, IMOBILIARIO})
    @DisplayName("Imobiliário também recusa favorecidos sem nenhum (formato) e soma um centavo abaixo (regra)")
    void recusaDeFavorecidosNosDoisTipos(String tipo) {
        abrirEConferirRecusado(ModeloPagamentoProcesso.favorecidosRecusadosPorFormato()
                .filter(c -> c.nome().equals("lista vazia")).findFirst().orElseThrow(),
                Resultado.RECUSA_DE_FORMATO, tipo);
        abrirEConferirRecusado(ModeloPagamentoProcesso.favorecidosRecusadosPorRegra()
                .filter(c -> c.nome().startsWith("soma um centavo abaixo")).findFirst().orElseThrow(),
                Resultado.RECUSA_DE_REGRA, tipo);
    }

    // Precedência: todo 400 de formato dos favorecidos vem antes de 404 e de 409.

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosRecusadosPorFormatoRepresentativos")
    @DisplayName("Favorecidos com formato inválido em cota inexistente: o 400 vem antes do 404")
    void favorecidosInvalidosVemAntesDeCotaInexistente(CasoFavorecidos caso) {
        tentarAbrirProcesso(INEXISTENTE, SERVICO, caso.valor(), caso.favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosRecusadosPorFormatoRepresentativos")
    @DisplayName("Favorecidos com formato inválido em cota com processo em andamento: o 400 vem antes do 409")
    void favorecidosInvalidosVemAntesDeCotaIndisponivel(CasoFavorecidos caso) {
        long cota = novaCota();
        abrirProcesso(cota, SERVICO);

        tentarAbrirProcesso(cota, SERVICO, caso.valor(), caso.favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("favorecidosRecusadosPorFormatoRepresentativos")
    @DisplayName("Favorecidos com formato inválido e valor acima do crédito: o 400 vem antes do 409")
    void favorecidosInvalidosVemAntesDeCreditoInsuficiente(CasoFavorecidos caso) {
        long cota = novaCota();
        BigDecimal acimaDoCredito = CREDITO_PADRAO.add(new BigDecimal("0.01"));

        tentarAbrirProcesso(cota, SERVICO, acimaDoCredito, caso.comValor(acimaDoCredito).favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));

        assertThat(quantidadeDeProcessosDaCota(cota), equalTo(0L));
    }

    // Consulta: GET /processos/{id}/favorecidos.

    @Test
    @DisplayName("Consulta devolve os favorecidos na ordem em que foram informados")
    void favorecidosVoltamNaOrdemInformada() {
        abrirEConferirAceito(ModeloPagamentoProcesso.favorecidosEmOrdemEmbaralhada(), SERVICO);
    }

    @Test
    @DisplayName("Favorecidos de processos diferentes não se confundem")
    void favorecidosDeProcessosDiferentesNaoSeConfundem() {
        CasoFavorecidos primeiro = ModeloPagamentoProcesso.exemploDoCasoDeUso();
        CasoFavorecidos segundo = ModeloPagamentoProcesso.favorecidosEmOrdemEmbaralhada();
        long processoUm = abrirProcesso(novaCota(), SERVICO, primeiro.valor(), primeiro.favorecidos());
        long processoDois = abrirProcesso(novaCota(), IMOBILIARIO, segundo.valor(), segundo.favorecidos());

        conferirFavorecidos(processoUm, primeiro.esperados());
        conferirFavorecidos(processoDois, segundo.esperados());
    }

    @Test
    @DisplayName("Consulta de favorecidos de processo inexistente é recusada com 404")
    void consultaDeFavorecidosEmProcessoInexistente() {
        tentarConsultarFavorecidos(INEXISTENTE).statusCode(404).body("mensagem", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosPorSituacao")
    @DisplayName("Os favorecidos continuam os mesmos em cada situação do processo")
    void favorecidosPermanecemEmCadaSituacao(Caminho caminho) {
        CasoFavorecidos caso = ModeloPagamentoProcesso.exemploDoCasoDeUso();
        long processo = abrirProcesso(novaCota(), caminho.tipo(), caso.valor(), caso.favorecidos());
        caminho.passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));

        conferirFavorecidos(processo, caso.esperados());
    }

    // ---- Fatia 4: alteração de valor e favorecidos, com concordância do cliente (R24 a R28) ----
    // Oráculo dos dados vigentes: ModeloPagamentoProcesso.ProcessoEsperado.

    /** Processo de uma cota própria, com o oráculo dos seus dados vigentes. */
    private record Acompanhado(long cota, long processo, ProcessoEsperado esperado, String tipo) {
    }

    /** Abre com o valor padrão e dois favorecidos conhecidos, na cota padrão. */
    private static Acompanhado aberto(String tipo) {
        long cota = novaCota();
        ProcessoEsperado esperado = ProcessoEsperado.inicial();
        long processo = abrirProcesso(cota, tipo, esperado.valor(), esperado.favorecidos());
        return new Acompanhado(cota, processo, esperado, tipo);
    }

    private static Acompanhado emAnalise(String tipo) {
        Acompanhado a = aberto(tipo);
        passo(a, INICIAR_ANALISE, EM_ANALISE);
        return a;
    }

    private static Acompanhado aguardandoConcordancia(String tipo) {
        Acompanhado a = emAnalise(tipo);
        passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
        return a;
    }

    /** Pede a ação aceita, confere o 200 e a situação e atualiza o oráculo. */
    private static void passo(Acompanhado a, String acao, String situacao) {
        aplicar(a.processo(), acao, a.esperado()).statusCode(200).body("estado", equalTo(situacao));
        a.esperado().confirmar(acao);
    }

    private static void conferir(Acompanhado a, String situacao) {
        conferirVigentes(a.cota(), a.processo(), a.esperado(), situacao);
        conferirHistoricoDeValores(a.processo(), a.esperado());
    }

    /**
     * Valor da listagem, favorecidos, crédito disponível da cota e proposta pendente são os esperados (R25, R26,
     * R27): em aguardando concordância valem os dados anteriores e a proposta mostra os novos; fora dela não há proposta.
     */
    private static void conferirVigentes(long cota, long processo, ProcessoEsperado esperado, String situacao) {
        conferirVigentes(cota, CREDITO_PADRAO, processo, esperado, situacao);
    }

    /** {@code creditoSemEsteProcesso}: crédito disponível da cota se este processo não estivesse pago. */
    private static void conferirVigentes(long cota, BigDecimal creditoSemEsteProcesso, long processo,
            ProcessoEsperado esperado, String situacao) {
        Map<String, Object> item = consultarProcesso(processo);
        assertThat(item.get("estado"), equalTo(situacao));
        assertThat(valorDoProcesso(item), comparesEqualTo(esperado.valor()));
        conferirFavorecidos(processo, esperado.favorecidos());
        BigDecimal credito = PAGO.equals(situacao) ? creditoSemEsteProcesso.subtract(esperado.valor())
                : creditoSemEsteProcesso;
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(credito));
        if (AGUARDANDO_CONCORDANCIA.equals(situacao)) {
            Proposta proposta = consultarProposta(processo);
            assertThat(proposta.valor(), comparesEqualTo(esperado.proposta().valor()));
            compararFavorecidos(proposta.favorecidos(), esperado.proposta().favorecidos());
            assertThat(proposta.motivo(), equalTo(esperado.motivoDaProposta()));
        } else {
            tentarConsultarProposta(processo).statusCode(404);
        }
    }

    /** R19: cada ação aceita no histórico, na ordem, com valor anterior e novo nas alterações e nulos nas demais. */
    private static void conferirHistoricoDeValores(long processo, ProcessoEsperado esperado) {
        List<Map<String, Object>> historico = consultarHistorico(processo);
        List<Registro> registros = esperado.historico();
        assertThat(historico.size(), equalTo(registros.size()));
        for (int i = 0; i < registros.size(); i++) {
            Map<String, Object> item = historico.get(i);
            Registro registro = registros.get(i);
            assertThat(item.get("acao"), equalTo(nomeNoHistorico(registro.acao())));
            conferirNumeroOuNulo(item.get("valorAnterior"), registro.valorAnterior());
            conferirNumeroOuNulo(item.get("valorNovo"), registro.valorNovo());
        }
    }

    private static void conferirNumeroOuNulo(Object obtido, BigDecimal esperado) {
        if (esperado == null) {
            assertThat(obtido, nullValue());
        } else {
            assertThat(obtido, notNullValue());
            assertThat(new BigDecimal(String.valueOf(obtido)), comparesEqualTo(esperado));
        }
    }

    static Stream<String> tipos() {
        return ModeloPagamentoProcesso.tipos();
    }

    // Máquina de estados: caminhos com repetição de propostas (R26, R27) e o cancelamento (R4, R28).

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Propor, discordar, propor de novo e concordar: cada resposta vale como o caso de uso manda")
    void proporDiscordarProporConcordar(String tipo) {
        Acompanhado a = emAnalise(tipo);
        conferir(a, EM_ANALISE);

        passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
        conferir(a, AGUARDANDO_CONCORDANCIA);
        BigDecimal primeiroValor = a.esperado().proposta().valor();

        passo(a, DISCORDAR, EM_ANALISE);
        conferir(a, EM_ANALISE);
        assertThat(a.esperado().valor(), comparesEqualTo(VALOR_PADRAO));

        passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
        conferir(a, AGUARDANDO_CONCORDANCIA);
        assertThat(a.esperado().proposta().valor(), org.hamcrest.Matchers.not(comparesEqualTo(primeiroValor)));

        passo(a, CONCORDAR, EM_ANALISE);
        conferir(a, EM_ANALISE);
        assertThat(a.esperado().valor(), comparesEqualTo(a.esperado().historico().get(3).valorNovo()));

        // R19: de e para de cada ação de alteração.
        List<Map<String, Object>> historico = consultarHistorico(a.processo());
        List<String> de = List.of(ABERTO, EM_ANALISE, AGUARDANDO_CONCORDANCIA, EM_ANALISE, AGUARDANDO_CONCORDANCIA);
        List<String> para = List.of(EM_ANALISE, AGUARDANDO_CONCORDANCIA, EM_ANALISE, AGUARDANDO_CONCORDANCIA, EM_ANALISE);
        for (int i = 0; i < de.size(); i++) {
            assertThat(historico.get(i).get("de"), equalTo(de.get(i)));
            assertThat(historico.get(i).get("para"), equalTo(para.get(i)));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("O backoffice propõe sem limite de vezes: cinco rodadas alternando discordar e concordar")
    void proporSemLimiteDeVezes(String tipo) {
        Acompanhado a = emAnalise(tipo);
        for (int rodada = 1; rodada <= 5; rodada++) {
            passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
            conferir(a, AGUARDANDO_CONCORDANCIA);
            passo(a, rodada % 2 == 0 ? CONCORDAR : DISCORDAR, EM_ANALISE);
            conferir(a, EM_ANALISE);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Propor e cancelar: a proposta é descartada, os dados anteriores ficam e a cota é liberada")
    void proporECancelar(String tipo) {
        Acompanhado a = aguardandoConcordancia(tipo);
        tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409);

        passo(a, CANCELAR, CANCELADO);

        conferir(a, CANCELADO);
        Map<String, Object> ultimo = consultarHistorico(a.processo()).getLast();
        assertThat(ultimo.get("de"), equalTo(AGUARDANDO_CONCORDANCIA));
        assertThat(ultimo.get("para"), equalTo(CANCELADO));
        assertThat(ultimo.get("observacao"), equalTo(motivoPadrao(CANCELAR)));
        long novo = abrirProcesso(a.cota(), SERVICO, CREDITO_PADRAO);
        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("O cliente altera várias vezes enquanto o processo está aberto: vale a última, e continua aberto")
    void alterarVariasVezesEmAberto(String tipo) {
        Acompanhado a = aberto(tipo);
        conferir(a, ABERTO);

        for (int vez = 1; vez <= 4; vez++) {
            passo(a, ALTERAR, ABERTO);
            conferir(a, ABERTO);
        }

        // R19: cada alteração no histórico com o valor vigente antes e o pedido; de e para são ABERTO.
        List<Map<String, Object>> historico = consultarHistorico(a.processo());
        assertThat(historico.size(), equalTo(4));
        assertThat(historico.stream().map(i -> i.get("de")).distinct().toList(), equalTo(List.of(ABERTO)));
        assertThat(historico.stream().map(i -> i.get("para")).distinct().toList(), equalTo(List.of(ABERTO)));
        assertThat(historico.stream().map(i -> i.get("acao")).distinct().toList(), equalTo(List.of("ALTERAR")));
        passo(a, INICIAR_ANALISE, EM_ANALISE);
        conferir(a, EM_ANALISE);
    }

    static Stream<Arguments> alteracoesAteOPagamento() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "o cliente concorda com a proposta", CONCORDAR),
                Arguments.of(tipo, "o cliente discorda da proposta", DISCORDAR)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("alteracoesAteOPagamento")
    @DisplayName("Alterar, propor e responder e seguir até pago: o crédito consumido é o do valor vigente final")
    void alteracoesAteOPagamentoConsomemOCreditoDoValorFinal(String tipo, String descricao, String resposta) {
        Acompanhado a = aberto(tipo);
        passo(a, ALTERAR, ABERTO);
        passo(a, ALTERAR, ABERTO);
        passo(a, INICIAR_ANALISE, EM_ANALISE);
        passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
        BigDecimal proposto = a.esperado().proposta().valor();
        BigDecimal vigenteAntes = a.esperado().valor();
        conferir(a, AGUARDANDO_CONCORDANCIA);
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        passo(a, resposta, EM_ANALISE);
        BigDecimal valorFinal = CONCORDAR.equals(resposta) ? proposto : vigenteAntes;
        assertThat(a.esperado().valor(), comparesEqualTo(valorFinal));
        conferir(a, EM_ANALISE);
        passo(a, CONCLUIR_ANALISE, IMOBILIARIO.equals(tipo) ? "AGUARDANDO_VISTORIA" : APROVADO);
        if (IMOBILIARIO.equals(tipo)) {
            passo(a, CONCLUIR_VISTORIA, APROVADO);
        }
        conferir(a, APROVADO);
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));

        passo(a, PAGAR, PAGO);

        conferir(a, PAGO);
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(valorFinal)));
    }

    // R26 e R27: resposta repetida ou cruzada é ação fora da ordem (R5), e nada muda.

    static Stream<Arguments> respostasRepetidas() {
        return tipos().flatMap(tipo -> Stream.of(CONCORDAR, DISCORDAR).map(resposta -> Arguments.of(tipo, resposta)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("respostasRepetidas")
    @DisplayName("Depois da resposta do cliente, concordar ou discordar de novo é recusado e nada muda")
    void respostaRepetidaERecusada(String tipo, String resposta) {
        Acompanhado a = aguardandoConcordancia(tipo);
        passo(a, resposta, EM_ANALISE);

        aplicar(a.processo(), CONCORDAR, a.esperado()).statusCode(409).body("mensagem", notNullValue());
        aplicarComMotivo(a.processo(), DISCORDAR, motivoPadrao(DISCORDAR)).statusCode(409)
                .body("mensagem", notNullValue());

        conferir(a, EM_ANALISE);
    }

    // R28: em aguardando concordância só concordar, discordar e cancelar; as demais ações são recusadas e a proposta fica.

    static Stream<Arguments> acoesRecusadasEmAguardandoConcordancia() {
        return tipos().flatMap(tipo -> ModeloPagamentoProcesso.acoesRecusadas(tipo, AGUARDANDO_CONCORDANCIA).stream()
                .map(acao -> Arguments.of(tipo, acao)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("acoesRecusadasEmAguardandoConcordancia")
    @DisplayName("Em aguardando concordância, as demais ações são recusadas e a proposta pendente continua a mesma")
    void recusaEmAguardandoConcordanciaPreservaAProposta(String tipo, String acao) {
        Acompanhado a = aguardandoConcordancia(tipo);

        aplicar(a.processo(), acao).statusCode(409).body("mensagem", notNullValue());

        conferir(a, AGUARDANDO_CONCORDANCIA);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Em aguardando concordância só concordar, discordar e cancelar são aceitos")
    void aguardandoConcordanciaAceitaSoTresAcoes(String tipo) {
        Acompanhado a = aguardandoConcordancia(tipo);

        assertThat(consultarAcoes(a.processo()), containsInAnyOrder("concordar", "discordar", "cancelar"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Consulta de ações: alterar só em aberto, propor-alteracao só em análise")
    void acoesDeAlteracaoPorSituacao(String tipo) {
        Acompanhado a = aberto(tipo);
        assertThat(consultarAcoes(a.processo()), hasItem("alterar"));
        assertThat(consultarAcoes(a.processo()), org.hamcrest.Matchers.not(hasItem("propor-alteracao")));

        passo(a, INICIAR_ANALISE, EM_ANALISE);

        assertThat(consultarAcoes(a.processo()), hasItem("propor-alteracao"));
        assertThat(consultarAcoes(a.processo()), org.hamcrest.Matchers.not(hasItem("alterar")));
        assertThat(consultarAcoes(a.processo()), org.hamcrest.Matchers.not(hasItem("concordar")));
        assertThat(consultarAcoes(a.processo()), org.hamcrest.Matchers.not(hasItem("discordar")));
    }

    @Test
    @DisplayName("Pedidos simultâneos de concordar e discordar: um só vale, e a situação e os dados são coerentes com ele")
    void concordarEDiscordarAoMesmoTempo() throws Exception {
        Acompanhado a = aguardandoConcordancia(SERVICO);
        int quantidade = 4;
        CyclicBarrier largada = new CyclicBarrier(quantidade);
        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            boolean concorda = i % 2 == 0;
            pedidos.add(() -> {
                largada.await(30, TimeUnit.SECONDS);
                return (concorda ? executar(a.processo(), CONCORDAR)
                        : executarComMotivo(a.processo(), DISCORDAR, "valor errado")).extract().statusCode();
            });
        }

        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
            for (Future<Integer> resposta : executor.invokeAll(pedidos)) {
                respostas.add(resposta.get());
            }
        }

        assertThat(Collections.frequency(respostas, 200), equalTo(1));
        assertThat(Collections.frequency(respostas, 409), equalTo(quantidade - 1));
        List<Map<String, Object>> historico = consultarHistorico(a.processo());
        assertThat(historico.size(), equalTo(a.esperado().historico().size() + 1));
        String vencedora = (String) historico.getLast().get("acao");
        a.esperado().confirmar("CONCORDAR".equals(vencedora) ? CONCORDAR : DISCORDAR);
        // O oráculo registrou a resposta vencedora; o histórico real tem o mesmo conteúdo, exceto o motivo do discordar.
        conferirVigentes(a.cota(), a.processo(), a.esperado(), EM_ANALISE);
        conferirHistoricoDeValores(a.processo(), a.esperado());
    }

    // Consulta da proposta: GET /processos/{id}/alteracao-proposta.

    @Test
    @DisplayName("Consulta da proposta de processo inexistente é recusada com 404")
    void propostaDeProcessoInexistente() {
        tentarConsultarProposta(INEXISTENTE).statusCode(404);
    }

    static Stream<Caminho> caminhosForaDeAguardandoConcordancia() {
        return ModeloPagamentoProcesso.caminhosPorSituacao()
                .filter(c -> !AGUARDANDO_CONCORDANCIA.equals(c.situacaoFinal()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosForaDeAguardandoConcordancia")
    @DisplayName("Fora de aguardando concordância não há proposta pendente: 404")
    void naoHaPropostaForaDeAguardandoConcordancia(Caminho caminho) {
        long processo = processoEm(caminho);

        tentarConsultarProposta(processo).statusCode(404);
    }

    static Stream<Arguments> respostasDaProposta() {
        return tipos().flatMap(tipo -> Stream.of(CONCORDAR, DISCORDAR, CANCELAR)
                .map(resposta -> Arguments.of(tipo, resposta)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("respostasDaProposta")
    @DisplayName("Depois de concordar, discordar ou cancelar não há mais proposta pendente: 404")
    void propostaSomeDepoisDaResposta(String tipo, String resposta) {
        Acompanhado a = aguardandoConcordancia(tipo);
        assertThat(consultarProposta(a.processo()).valor(), comparesEqualTo(a.esperado().proposta().valor()));

        passo(a, resposta, CONCORDAR.equals(resposta) || DISCORDAR.equals(resposta) ? EM_ANALISE : CANCELADO);

        tentarConsultarProposta(a.processo()).statusCode(404);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("A proposta mostra novo valor, novos favorecidos na ordem informada e o motivo; os vigentes seguem os anteriores")
    void propostaMostraOsNovosDadosEOsVigentesSeguemOsAnteriores(String tipo) {
        Acompanhado a = emAnalise(tipo);
        CasoFavorecidos caso = ModeloPagamentoProcesso.favorecidosEmOrdemEmbaralhada();
        String motivo = "valor errado no pedido";

        tentarProporAlteracao(a.processo(), caso.valor(), caso.favorecidos(), motivo).statusCode(200)
                .body("estado", equalTo(AGUARDANDO_CONCORDANCIA));
        a.esperado().propor(new Alteracao(caso.valor(), caso.esperados()), motivo);

        Proposta proposta = consultarProposta(a.processo());
        assertThat(proposta.valor(), comparesEqualTo(caso.valor()));
        compararFavorecidos(proposta.favorecidos(), caso.esperados());
        assertThat(proposta.motivo(), equalTo(motivo));
        // Vigentes: os anteriores, na listagem, nos favorecidos e no crédito disponível.
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(VALOR_PADRAO));
        conferirFavorecidos(a.processo(), ProcessoEsperado.inicial().favorecidos());
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));

        executar(a.processo(), CONCORDAR).statusCode(200).body("estado", equalTo(EM_ANALISE));
        a.esperado().concordar();

        conferirVigentes(a.cota(), a.processo(), a.esperado(), EM_ANALISE);
        conferirFavorecidos(a.processo(), caso.esperados());
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(caso.valor()));
    }

    @Test
    @DisplayName("Proposta pendente não consome crédito: com 100.000 pagos, propor 150.000,00 deixa 150.000,00 disponíveis")
    void propostaPendenteNaoConsomeCredito() {
        long cota = novaCota(CREDITO_PADRAO);
        long pago = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        caminhoAte(SERVICO, PAGO).passos().forEach(p -> aplicar(pago, p.acao()).statusCode(200));
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        executar(processo, INICIAR_ANALISE).statusCode(200);
        BigDecimal disponivel = new BigDecimal("150000.00");
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(disponivel));

        tentarProporAlteracao(processo, disponivel, favorecidoUnico(disponivel), "ajuste de valor").statusCode(200)
                .body("estado", equalTo(AGUARDANDO_CONCORDANCIA));

        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(disponivel));
        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(new BigDecimal("100000.00")));
        executar(processo, CONCORDAR).statusCode(200);
        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(disponivel));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(disponivel));
    }

    @Test
    @DisplayName("Listagem por aguardando concordância acompanha a resposta do cliente")
    void listagemAcompanhaARespostaDoCliente() {
        Acompanhado a = aguardandoConcordancia(SERVICO);
        assertThat(idsDaListagem(AGUARDANDO_CONCORDANCIA), hasItem(a.processo()));
        assertThat(idsDaListagem(EM_ANALISE), org.hamcrest.Matchers.not(hasItem(a.processo())));

        passo(a, DISCORDAR, EM_ANALISE);

        assertThat(idsDaListagem(AGUARDANDO_CONCORDANCIA), org.hamcrest.Matchers.not(hasItem(a.processo())));
        assertThat(idsDaListagem(EM_ANALISE), hasItem(a.processo()));
    }

    private static List<Long> idsDaListagem(String situacao) {
        return listarProcessosPorSituacao(situacao).stream().map(i -> ((Number) i.get("id")).longValue()).toList();
    }

    // R24 e R25: valor e favorecidos na alteração seguem R12, R13, R15 e R29, como na abertura.

    /** Alteração do cliente (processo aberto) ou proposta do backoffice (processo em análise). */
    enum Via {
        CLIENTE_EM_ABERTO(ALTERAR, ABERTO, ABERTO),
        BACKOFFICE_EM_ANALISE(PROPOR_ALTERACAO, EM_ANALISE, AGUARDANDO_CONCORDANCIA);

        final String acao;
        final String situacaoAntes;
        final String situacaoDepois;

        Via(String acao, String situacaoAntes, String situacaoDepois) {
            this.acao = acao;
            this.situacaoAntes = situacaoAntes;
            this.situacaoDepois = situacaoDepois;
        }

        Acompanhado preparar(String tipo) {
            return preparar(novaCota(), tipo, ProcessoEsperado.inicial());
        }

        /** Abre o processo na cota com os dados do oráculo e o leva à situação em que esta via vale. */
        Acompanhado preparar(long cota, String tipo, ProcessoEsperado esperado) {
            long processo = abrirProcesso(cota, tipo, esperado.valor(), esperado.favorecidos());
            Acompanhado a = new Acompanhado(cota, processo, esperado, tipo);
            if (this == BACKOFFICE_EM_ANALISE) {
                passo(a, INICIAR_ANALISE, EM_ANALISE);
            }
            return a;
        }

        /** Processo numa situação em que esta via é recusada com 409 (ação fora da ordem). */
        Acompanhado prepararEmSituacaoQueARecusa(String tipo) {
            return this == CLIENTE_EM_ABERTO ? emAnalise(tipo) : aberto(tipo);
        }

        ValidatableResponse pedir(long processo, Object valor, Object favorecidos) {
            return this == CLIENTE_EM_ABERTO ? tentarAlterar(processo, valor, favorecidos)
                    : tentarProporAlteracao(processo, valor, favorecidos, "motivo da proposta");
        }

        /** Registra no oráculo o pedido aceito. */
        void registrar(ProcessoEsperado esperado, Alteracao alteracao) {
            if (this == CLIENTE_EM_ABERTO) {
                esperado.alterar(alteracao);
            } else {
                esperado.propor(alteracao, "motivo da proposta");
            }
        }
    }

    static Stream<Via> vias() {
        return Stream.of(Via.values());
    }

    private static <T> Stream<Arguments> comAsVias(Stream<T> casos) {
        List<T> lista = casos.toList();
        return Stream.of(Via.values()).flatMap(via -> lista.stream().map(caso -> Arguments.of(via, caso)));
    }

    /**
     * Na alteração, R15 e R29 não são refeitas caso a caso (a abertura já cobre a partição inteira): vale a mesma
     * regra, e aqui se confere uma amostra de cada classe, com os valores-limite da soma, nas duas vias.
     */
    private static Stream<CasoFavorecidos> amostra(Stream<CasoFavorecidos> casos, String... inicios) {
        return casos.filter(caso -> java.util.Arrays.stream(inicios).anyMatch(caso.nome()::startsWith));
    }

    static Stream<Arguments> viasComFavorecidosAceitos() {
        return comAsVias(amostra(ModeloPagamentoProcesso.favorecidosAceitos(), "exemplo do caso de uso",
                "um favorecido (CPF)", "dez favorecidos", "cem favorecidos", "rateio com centavos",
                "parte de 0,01 e o resto"));
    }

    static Stream<Arguments> viasComFavorecidosRecusadosPorFormato() {
        return comAsVias(amostra(ModeloPagamentoProcesso.favorecidosRecusadosPorFormato(), "lista ausente",
                "lista vazia", "nome vazio", "documento com 10 dígitos", "papel com 51 caracteres", "parte nula",
                "parte 0,00", "parte com três casas: 40,005"));
    }

    static Stream<Arguments> viasComFavorecidosRecusadosPorRegra() {
        return comAsVias(amostra(ModeloPagamentoProcesso.favorecidosRecusadosPorRegra(),
                "soma um centavo abaixo: ", "soma um centavo acima: ", "rateio com centavos: 33,33 x 3",
                "mesmo CPF duas vezes, com o mesmo nome", "mesmo CNPJ duas vezes"));
    }

    static Stream<Arguments> viasComTabelaDeDecisao() {
        return comAsVias(ModeloPagamentoProcesso.tabelaDeDecisao());
    }

    static Stream<Arguments> viasComFormatoRepresentativo() {
        return comAsVias(amostra(ModeloPagamentoProcesso.favorecidosRecusadosPorFormatoRepresentativos(),
                "lista vazia", "parte 0,00"));
    }

    /** O modelo manda aceitar: a via aceita e os dados novos passam a valer (alterar) ou a ser propostos (propor). */
    private static void alterarEConferirAceito(Via via, CasoFavorecidos caso) {
        assertThat("o modelo manda aceitar", caso.resultado(), equalTo(Resultado.ACEITO));
        Acompanhado a = via.preparar(SERVICO);

        via.pedir(a.processo(), caso.valor(), caso.favorecidos()).statusCode(200)
                .body("estado", equalTo(via.situacaoDepois));
        via.registrar(a.esperado(), new Alteracao(caso.valor(), caso.esperados()));

        conferir(a, via.situacaoDepois);
    }

    /** O modelo manda recusar: 400 com mensagem (e campos de favorecidos, se de formato) e nada muda. */
    private static void alterarEConferirRecusado(Via via, CasoFavorecidos caso, Resultado esperado, String tipo) {
        assertThat("o modelo manda recusar", caso.resultado(), equalTo(esperado));
        Acompanhado a = via.preparar(tipo);

        ValidatableResponse resposta = via.pedir(a.processo(), caso.valor(), caso.favorecidos()).statusCode(400)
                .body("mensagem", notNullValue());
        if (esperado == Resultado.RECUSA_DE_FORMATO) {
            resposta.body("campos.campo", hasItem(containsString("favorecidos")));
        }

        // Os dados anteriores ficam, e a via continua aberta a um pedido válido.
        conferir(a, via.situacaoAntes);
        Alteracao valida = Alteracao.numero(1);
        via.pedir(a.processo(), valida.valor(), valida.favorecidos()).statusCode(200)
                .body("estado", equalTo(via.situacaoDepois));
        via.registrar(a.esperado(), valida);
        conferir(a, via.situacaoDepois);
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFavorecidosAceitos")
    @DisplayName("Alteração com favorecidos válidos somando o valor é aceita e os vigentes e a proposta ficam como R24 a R26 mandam")
    void alteracaoComFavorecidosValidosEAceita(Via via, CasoFavorecidos caso) {
        alterarEConferirAceito(via, caso);
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFavorecidosRecusadosPorFormato")
    @DisplayName("Alteração com favorecido ausente, em branco, fora do tamanho ou do formato: 400 com campos, nada muda")
    void alteracaoComFormatoInvalidoDeFavorecidosERecusada(Via via, CasoFavorecidos caso) {
        alterarEConferirRecusado(via, caso, Resultado.RECUSA_DE_FORMATO, SERVICO);
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFavorecidosRecusadosPorRegra")
    @DisplayName("Alteração com soma diferente do valor ou documento repetido: 400 com mensagem, nada muda")
    void alteracaoQueFereARegraDeFavorecidosERecusada(Via via, CasoFavorecidos caso) {
        alterarEConferirRecusado(via, caso, Resultado.RECUSA_DE_REGRA, SERVICO);
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComTabelaDeDecisao")
    @DisplayName("Tabela de decisão de R15 e R29 na alteração: cada condição, sozinha, decide")
    void tabelaDeDecisaoDosFavorecidosNaAlteracao(Via via, CasoFavorecidos caso) {
        switch (caso.resultado()) {
            case ACEITO -> alterarEConferirAceito(via, caso);
            case RECUSA_DE_FORMATO -> alterarEConferirRecusado(via, caso, Resultado.RECUSA_DE_FORMATO, SERVICO);
            case RECUSA_DE_REGRA -> alterarEConferirRecusado(via, caso, Resultado.RECUSA_DE_REGRA, SERVICO);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Nos dois tipos: favorecidos sem nenhum (formato) e soma um centavo abaixo (regra) são recusados")
    void alteracaoRecusadaNosDoisTipos(String tipo) {
        for (Via via : Via.values()) {
            alterarEConferirRecusado(via, ModeloPagamentoProcesso.favorecidosRecusadosPorFormato()
                    .filter(c -> c.nome().equals("lista vazia")).findFirst().orElseThrow(),
                    Resultado.RECUSA_DE_FORMATO, tipo);
            alterarEConferirRecusado(via, ModeloPagamentoProcesso.favorecidosRecusadosPorRegra()
                    .filter(c -> c.nome().startsWith("soma um centavo abaixo")).findFirst().orElseThrow(),
                    Resultado.RECUSA_DE_REGRA, tipo);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vias")
    @DisplayName("Exemplo do caso de uso na alteração (vendedor 9.500,00 e cartório 500,00) é aceito")
    void exemploDoCasoDeUsoNaAlteracao(Via via) {
        alterarEConferirAceito(via, ModeloPagamentoProcesso.exemploDoCasoDeUso());
    }

    // Particionamento do valor na alteração (R12).

    static Stream<Arguments> viasComValoresValidos() {
        return Stream.of(Via.values()).flatMap(via -> valoresValidos()
                .filter(caso -> List.of("limite inferior: 0,01", "duas casas decimais", "exatamente o crédito da cota")
                        .contains(caso.get()[0]))
                .map(caso -> {
            Object[] dados = caso.get();
            return Arguments.of(via, dados[0], dados[2]);
        }));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComValoresValidos")
    @DisplayName("Valor maior que zero com até duas casas, até o crédito, é aceito na alteração")
    void alteracaoComValorValidoEAceita(Via via, String descricao, BigDecimal valor) {
        Acompanhado a = via.preparar(SERVICO);
        Alteracao alteracao = new Alteracao(valor, favorecidoUnico(valor));

        via.pedir(a.processo(), alteracao.valor(), alteracao.favorecidos()).statusCode(200)
                .body("estado", equalTo(via.situacaoDepois));
        via.registrar(a.esperado(), alteracao);

        conferir(a, via.situacaoDepois);
    }

    static Stream<Arguments> viasComValoresInvalidos() {
        return Stream.of(Via.values()).flatMap(via -> valoresInvalidos()
                .filter(caso -> List.of("ausente", "nulo", "zero", "três casas: 100,005").contains(caso.get()[0]))
                .map(caso -> {
            Object[] dados = caso.get();
            return Arguments.of(via, dados[0], dados[1]);
        }));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComValoresInvalidos")
    @DisplayName("Valor ausente, nulo, zero, negativo ou com mais de duas casas é recusado na alteração, com 400 e campo valor")
    void alteracaoComValorInvalidoERecusada(Via via, String descricao, String valorJson) {
        Object valor = valorJson == null ? AUSENTE : "null".equals(valorJson) ? null : new BigDecimal(valorJson);
        Acompanhado a = via.preparar(SERVICO);

        via.pedir(a.processo(), valor, favorecidoUnico(new BigDecimal("100.00"))).statusCode(400)
                .body("mensagem", notNullValue())
                .body("campos.campo", hasItem("valor"));

        conferir(a, via.situacaoAntes);
    }

    // Valor-limite do crédito na alteração (R13): exatamente o disponível aceito, um centavo acima recusado (409).

    static Stream<Arguments> viasComCenariosDeCreditoRestante() {
        List<String> amostra = List.of("cota sem processo", "pago 100000 (serviço)",
                "dois pagos somam: 100000 e 50000", "crédito com centavos: pago 500,25 de 1000,50");
        return Stream.of(Via.values()).flatMap(via -> cenariosComCotaLivreECreditoRestante()
                .filter(cenario -> amostra.contains(cenario.nome()))
                .map(cenario -> Arguments.of(via, cenario)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComCenariosDeCreditoRestante")
    @DisplayName("Alterar ou propor exatamente o crédito disponível é aceito; a proposta pendente ainda não consome crédito")
    void valorIgualAoCreditoDisponivelNaAlteracaoEAceito(Via via, CenarioCredito cenario) {
        long cota = cotaDoCenario(cenario);
        BigDecimal disponivel = cenario.creditoDisponivelEsperado();
        Acompanhado a = via.preparar(cota, SERVICO, ProcessoEsperado.comUmFavorecido(new BigDecimal("0.01")));
        Alteracao exata = new Alteracao(disponivel, favorecidoUnico(disponivel));

        via.pedir(a.processo(), exata.valor(), exata.favorecidos()).statusCode(200)
                .body("estado", equalTo(via.situacaoDepois));
        via.registrar(a.esperado(), exata);

        conferirVigentes(cota, disponivel, a.processo(), a.esperado(), via.situacaoDepois);
        conferirHistoricoDeValores(a.processo(), a.esperado());
        if (via == Via.BACKOFFICE_EM_ANALISE) {
            passo(a, CONCORDAR, EM_ANALISE);
            conferirVigentes(cota, disponivel, a.processo(), a.esperado(), EM_ANALISE);
            assertThat(a.esperado().valor(), comparesEqualTo(disponivel));
        }
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComCenariosDeCreditoRestante")
    @DisplayName("Alterar ou propor um centavo acima do crédito disponível é recusado com 409 e nada muda")
    void valorUmCentavoAcimaDoCreditoNaAlteracaoERecusado(Via via, CenarioCredito cenario) {
        long cota = cotaDoCenario(cenario);
        BigDecimal disponivel = cenario.creditoDisponivelEsperado();
        Acompanhado a = via.preparar(cota, SERVICO, ProcessoEsperado.comUmFavorecido(new BigDecimal("0.01")));
        BigDecimal acima = disponivel.add(new BigDecimal("0.01"));

        via.pedir(a.processo(), acima, favorecidoUnico(acima)).statusCode(409).body("mensagem", notNullValue());

        conferirVigentes(cota, disponivel, a.processo(), a.esperado(), via.situacaoAntes);
        conferirHistoricoDeValores(a.processo(), a.esperado());
        // A recusa não travou o processo: o valor exato é aceito em seguida.
        Alteracao exata = new Alteracao(disponivel, favorecidoUnico(disponivel));
        via.pedir(a.processo(), exata.valor(), exata.favorecidos()).statusCode(200)
                .body("estado", equalTo(via.situacaoDepois));
    }

    @Test
    @DisplayName("Exemplo de R13 na alteração: cota de 250.000 com 100.000 pagos aceita alterar para 140.000 e recusa 150.000,01")
    void exemploDoCasoDeUsoDeCreditoNaAlteracao() {
        long cota = novaCota(CREDITO_PADRAO);
        long pago = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        caminhoAte(SERVICO, PAGO).passos().forEach(p -> aplicar(pago, p.acao()).statusCode(200));
        ProcessoEsperado esperado = ProcessoEsperado.comUmFavorecido(new BigDecimal("100000.00"));
        long processo = abrirProcesso(cota, SERVICO, esperado.valor(), esperado.favorecidos());
        Acompanhado a = new Acompanhado(cota, processo, esperado, SERVICO);
        BigDecimal disponivel = new BigDecimal("150000.00");
        Alteracao aceita = new Alteracao(new BigDecimal("140000.00"), favorecidoUnico(new BigDecimal("140000.00")));

        tentarAlterar(processo, aceita.valor(), aceita.favorecidos()).statusCode(200).body("estado", equalTo(ABERTO));
        esperado.alterar(aceita);
        conferirVigentes(cota, disponivel, processo, esperado, ABERTO);

        BigDecimal acima = new BigDecimal("150000.01");
        tentarAlterar(processo, acima, favorecidoUnico(acima)).statusCode(409).body("mensagem", notNullValue());

        conferirVigentes(cota, disponivel, processo, esperado, ABERTO);
        conferirHistoricoDeValores(processo, esperado);
        assertThat(a.esperado().valor(), comparesEqualTo(new BigDecimal("140000.00")));
    }

    @Test
    @DisplayName("Exemplo de R24: aberto de 30.000, alterar para 28.000 com um favorecido de 28.000 vale; com favorecidos somando 27.000 é recusado")
    void exemploDoCasoDeUsoDeAlteracaoEmAberto() {
        Acompanhado a = aberto(SERVICO);
        BigDecimal novoValor = new BigDecimal("28000.00");
        Alteracao boa = new Alteracao(novoValor, favorecidoUnico(novoValor));

        tentarAlterar(a.processo(), boa.valor(), boa.favorecidos()).statusCode(200).body("estado", equalTo(ABERTO));
        a.esperado().alterar(boa);
        conferir(a, ABERTO);

        tentarAlterar(a.processo(), novoValor, favorecidoUnico(new BigDecimal("27000.00"))).statusCode(400)
                .body("mensagem", notNullValue());

        conferir(a, ABERTO);
    }

    @Test
    @DisplayName("Exemplo de R25: em análise de 30.000, propor 25.000 com motivo vai a aguardando concordância e o vigente segue 30.000")
    void exemploDoCasoDeUsoDePropostaEmAnalise() {
        Acompanhado a = emAnalise(SERVICO);
        BigDecimal novoValor = new BigDecimal("25000.00");
        Alteracao proposta = new Alteracao(novoValor, favorecidoUnico(novoValor));

        tentarProporAlteracao(a.processo(), proposta.valor(), proposta.favorecidos(), "valor errado").statusCode(200)
                .body("estado", equalTo(AGUARDANDO_CONCORDANCIA));
        a.esperado().propor(proposta, "valor errado");

        conferir(a, AGUARDANDO_CONCORDANCIA);
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(VALOR_PADRAO));
    }

    // Precedência (contrato da fatia 4): todo 400 de FORMATO vem antes de 404 e de 409.

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFormatoRepresentativo")
    @DisplayName("Favorecidos com formato inválido em processo inexistente: o 400 vem antes do 404")
    void formatoDeFavorecidosNaAlteracaoVemAntesDoProcessoInexistente(Via via, CasoFavorecidos caso) {
        via.pedir(INEXISTENTE, caso.valor(), caso.favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFormatoRepresentativo")
    @DisplayName("Favorecidos com formato inválido em situação que recusa a ação: o 400 vem antes do 409")
    void formatoDeFavorecidosNaAlteracaoVemAntesDaSituacao(Via via, CasoFavorecidos caso) {
        Acompanhado a = via.prepararEmSituacaoQueARecusa(SERVICO);
        String situacao = situacaoQueARecusa(via);

        via.pedir(a.processo(), caso.valor(), caso.favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));

        conferir(a, situacao);
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("viasComFormatoRepresentativo")
    @DisplayName("Favorecidos com formato inválido e valor acima do crédito: o 400 vem antes do 409")
    void formatoDeFavorecidosNaAlteracaoVemAntesDoCredito(Via via, CasoFavorecidos caso) {
        Acompanhado a = via.preparar(SERVICO);
        BigDecimal acimaDoCredito = CREDITO_PADRAO.add(new BigDecimal("0.01"));

        via.pedir(a.processo(), acimaDoCredito, caso.comValor(acimaDoCredito).favorecidos()).statusCode(400)
                .body("campos.campo", hasItem(containsString("favorecidos")));

        conferir(a, via.situacaoAntes);
    }

    /** Situação em que {@link Via#prepararEmSituacaoQueARecusa} deixa o processo. */
    private static String situacaoQueARecusa(Via via) {
        return via == Via.CLIENTE_EM_ABERTO ? EM_ANALISE : ABERTO;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("vias")
    @DisplayName("Valor inválido em processo inexistente e em situação que recusa a ação: o 400 vem antes do 404 e do 409")
    void valorInvalidoNaAlteracaoVemAntesDe404E409(Via via) {
        via.pedir(INEXISTENTE, BigDecimal.ZERO, favorecidoUnico(new BigDecimal("100.00"))).statusCode(400)
                .body("campos.campo", hasItem("valor"));
        Acompanhado a = via.prepararEmSituacaoQueARecusa(SERVICO);

        via.pedir(a.processo(), BigDecimal.ZERO, favorecidoUnico(new BigDecimal("100.00"))).statusCode(400)
                .body("campos.campo", hasItem("valor"));

        conferir(a, situacaoQueARecusa(via));
    }

    @Test
    @DisplayName("Propor sem motivo: o 400 com campo motivo vem antes do 404 e do 409")
    void motivoAusenteNaPropostaVemAntesDe404E409() {
        Alteracao alteracao = Alteracao.numero(1);
        tentarProporAlteracao(INEXISTENTE, alteracao.valor(), alteracao.favorecidos(), AUSENTE).statusCode(400)
                .body("campos.campo", hasItem("motivo"));
        Acompanhado a = aberto(SERVICO);

        tentarProporAlteracao(a.processo(), alteracao.valor(), alteracao.favorecidos(), AUSENTE).statusCode(400)
                .body("campos.campo", hasItem("motivo"));

        conferir(a, ABERTO);
    }

    @Test
    @DisplayName("Discordar sem motivo: o 400 com campo motivo vem antes do 404 e do 409")
    void motivoAusenteNaDiscordanciaVemAntesDe404E409() {
        aplicarComMotivo(INEXISTENTE, DISCORDAR, AUSENTE).statusCode(400).body("campos.campo", hasItem("motivo"));
        Acompanhado a = emAnalise(SERVICO);

        aplicarComMotivo(a.processo(), DISCORDAR, AUSENTE).statusCode(400).body("campos.campo", hasItem("motivo"));

        conferir(a, EM_ANALISE);
    }


    /**
     * Ação com o corpo que o contrato exige: motivo nas ações que o pedem, valor e favorecidos (e motivo) nas
     * alterações, nenhum corpo nas demais. As alterações levam dados válidos (R12, R13, R15, R29).
     */
    private static ValidatableResponse aplicar(long processo, String acao) {
        return aplicar(processo, acao, ProcessoEsperado.inicial());
    }

    /** Como {@link #aplicar(long, String)}, com o oráculo guardando o pedido (confirmar só depois do 200). */
    private static ValidatableResponse aplicar(long processo, String acao, ProcessoEsperado esperado) {
        Map<String, Object> corpo = esperado.preparar(acao);
        return corpo == null ? executar(processo, acao) : executarComCorpo(processo, acao, json(corpo));
    }

    /** Ação de motivo obrigatório com o motivo dado (ou AUSENTE); o resto do corpo é válido. */
    private static ValidatableResponse aplicarComMotivo(long processo, String acao, Object motivo) {
        return executarComCorpo(processo, acao, json(ModeloPagamentoProcesso.corpoComMotivo(acao, motivo)));
    }

    private static long processoEm(Caminho caminho) {
        long processo = abrirProcesso(novaCota(), caminho.tipo());
        caminho.passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));
        return processo;
    }

    private static long processoEm(Base base) {
        return processoEm(caminhoAte(base.tipo(), base.situacao()));
    }

    private static long processoAprovado() {
        long processo = abrirProcesso(novaCota(), SERVICO);
        executar(processo, INICIAR_ANALISE).statusCode(200);
        executar(processo, CONCLUIR_ANALISE).statusCode(200).body("estado", equalTo(APROVADO));
        return processo;
    }
}
