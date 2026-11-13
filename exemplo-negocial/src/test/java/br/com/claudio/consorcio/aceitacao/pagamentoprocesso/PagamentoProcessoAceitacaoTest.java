package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.abrirProcesso;
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
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.SERVICO;
import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.claudio.consorcio.aceitacao.TesourariaStub;
import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AcaoInvalida;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Base;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Caminho;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Passo;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
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

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosCompletos")
    @DisplayName("Fluxo de negócio chega à situação final")
    void fluxoChegaASituacaoFinal(Caminho caminho) {
        long processo = abrirProcesso(novaCota(), caminho.tipo());
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));

        for (Passo passo : caminho.passos()) {
            aplicar(processo, passo.acao()).statusCode(200).body("estado", equalTo(passo.situacao()));
            assertThat(consultarProcesso(processo).get("estado"), equalTo(passo.situacao()));
        }

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
        caminho.passos().forEach(p -> aplicar(processo, p.acao()).statusCode(200));

        enviar("/processos", Map.of("cotaId", cota, "tipo", SERVICO)).statusCode(409)
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

        executarComCorpo(processo, base.acao(), "{}").statusCode(400)
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

        executarComMotivo(processo, base.acao(), motivo).statusCode(400)
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

        executarComMotivo(processo, base.acao(), motivo).statusCode(200).body("estado", equalTo(base.destino()));

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
    @ValueSource(strings = {PENDENTE_DOCUMENTO, REPROVADO})
    @DisplayName("Listagem filtra pelas situações pendente de documento e reprovado")
    void listagemFiltraPorSituacoesNovas(String situacao) {
        long processo = processoEm(caminhoAte(SERVICO, situacao));

        List<Map<String, Object>> itens = listarProcessosPorSituacao(situacao);

        assertThat(itens.stream().map(i -> ((Number) i.get("id")).longValue()).toList(), hasItem(processo));
        assertThat(itens.stream().map(i -> i.get("estado")).distinct().toList(), equalTo(List.of(situacao)));
    }

    /** Ação com o corpo que o contrato exige: motivo nas ações que o pedem, nenhum corpo nas demais. */
    private static ValidatableResponse aplicar(long processo, String acao) {
        return exigeMotivo(acao) ? executarComMotivo(processo, acao, motivoPadrao(acao)) : executar(processo, acao);
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
