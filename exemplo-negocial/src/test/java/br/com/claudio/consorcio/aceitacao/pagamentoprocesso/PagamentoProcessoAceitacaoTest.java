package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.AUSENTE;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.TENTAR_PAGAMENTO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.aguardar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.aguardarSituacao;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.aguardarTentativas;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarHistoricoSemTentativas;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.quantidadeDeTentativas;
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
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.requisicaoDeCliente;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.requisicaoDeConsulta;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ABERTO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AGUARDANDO_CONCORDANCIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AGUARDANDO_ALCADA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AGUARDANDO_VISTORIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ALTERAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.APROVAR_NA_ALCADA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.VALOR_ACIMA_DA_ALCADA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.decisaoDaAlcada;
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
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGAMENTO_SOLICITADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PAGO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.MOTIVO_DA_TESOURARIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.PROTOCOLO_DA_TESOURARIA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ROTINA_CONFIRMOU;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ROTINA_RECUSOU;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.SOLICITAR_PAGAMENTO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.DEVOLVER_PARA_ANALISE;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CANCELADO;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CANCELAR;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.SERVICO;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.comparesEqualTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import br.com.claudio.consorcio.aceitacao.ApiAceitacao.Proposta;
import br.com.claudio.consorcio.aceitacao.TesourariaStub;
import br.com.claudio.consorcio.aceitacao.TesourariaStub.OrdemRecebida;
import br.com.claudio.consorcio.aceitacao.TesourariaStub.Parte;
import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.AcaoInvalida;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Alteracao;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ProcessoEsperado;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Registro;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Base;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Caminho;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Faixa;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CasoFavorecidos;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.CenarioCredito;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ProcessoPlanejado;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Passo;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Resposta;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Resultado;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.time.Duration;
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
import org.junit.jupiter.api.AfterEach;
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

    /** A tesouraria começa e termina cada caso confirmando: a rotina roda sobre todos os processos, de todos os casos. */
    @BeforeEach
    void tesourariaNoAr() {
        TesourariaStub.confirmar();
    }

    @AfterEach
    void tesourariaDeVolta() {
        TesourariaStub.confirmar();
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

    static Stream<Caminho> caminhosCompletosPelaAlcada() {
        return ModeloPagamentoProcesso.caminhosCompletosPelaAlcada();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosCompletosPelaAlcada")
    @DisplayName("Fluxo de um processo acima da alçada passa por aguardando alçada e chega à situação final")
    void fluxoPelaAlcadaChegaASituacaoFinal(Caminho caminho) {
        percorrerEConferir(caminho);
    }

    private static void percorrerEConferir(Caminho caminho) {
        long cota = novaCota();
        ProcessoEsperado esperado = ProcessoEsperado.comValor(caminho.valor());
        long processo = abrirProcesso(cota, caminho.tipo(), esperado.valor(), esperado.favorecidos());
        assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));

        for (Passo passo : caminho.passos()) {
            switch (passo.acao()) {
                case ROTINA_CONFIRMOU -> {
                    TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
                    aguardarSituacao(processo, PAGO);
                }
                case ROTINA_RECUSOU -> {
                    TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
                    aguardarSituacao(processo, APROVADO);
                    TesourariaStub.confirmar();
                }
                case SOLICITAR_PAGAMENTO -> {
                    // A tesouraria fica fora do ar até conferir o pagamento solicitado: a rotina não tira o processo de lá.
                    TesourariaStub.ficarIndisponivel();
                    aplicar(processo, passo.acao(), esperado).statusCode(200).body("estado", equalTo(passo.situacao()));
                }
                default -> aplicar(processo, passo.acao(), esperado).statusCode(200)
                        .body("estado", equalTo(passo.situacao()));
            }
            esperado.confirmar(passo.acao());
            // R24 a R28: a cada passo, a situação, o valor, os favorecidos, o crédito e a proposta pendente
            // são os vigentes esperados.
            conferirVigentes(cota, processo, esperado, passo.situacao());
        }
        conferirHistoricoDeValores(processo, esperado);

        // R19: cada ação aceita e cada resultado da rotina está no histórico, em ordem, com o motivo.
        List<Map<String, Object>> historico = consultarHistoricoSemTentativas(processo);
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
            if (ROTINA_RECUSOU.equals(passo.acao())) {
                assertThat(item.get("observacao"), equalTo(MOTIVO_DA_TESOURARIA));
            }
            Instant quando = Instant.parse((String) item.get("quando"));
            assertThat(quando, greaterThanOrEqualTo(ultimo));
            ultimo = quando;
            anterior = passo.situacao();
        }
        // R20: o protocolo só existe depois de pago, e é o que a tesouraria devolveu.
        Object protocolo = consultarProcesso(processo).get("protocoloPagamento");
        if (PAGO.equals(caminho.situacaoFinal())) {
            assertThat(protocolo, equalTo(PROTOCOLO_DA_TESOURARIA));
        } else {
            assertThat(protocolo, nullValue());
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
        long processo = abrirProcesso(novaCota(), invalida.ate().tipo(), invalida.ate().valor());
        levar(processo, invalida.ate());
        int itensNoHistorico = consultarHistoricoSemTentativas(processo).size();

        // Motivo válido nas ações que o exigem, para que só a ordem seja o problema.
        aplicar(processo, invalida.acao()).statusCode(409).body("mensagem", notNullValue());

        assertThat(consultarProcesso(processo).get("estado"), equalTo(invalida.ate().situacaoFinal()));
        assertThat(consultarHistoricoSemTentativas(processo).size(), equalTo(itensNoHistorico));
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
        long processo = abrirProcesso(cota, caminho.tipo(), caminho.valor());
        levar(processo, caminho);

        tentarAbrirProcesso(cota, SERVICO).statusCode(409)
                .body("mensagem", notNullValue());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosFinalizados")
    @DisplayName("Cota com processo finalizado pode abrir outro")
    void cotaComProcessoFinalizadoAbreOutro(Caminho caminho) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, caminho.tipo(), caminho.valor());
        levar(processo, caminho);

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

    // Pagamento como saga (R7, R8, R20 a R22, R31): ver a seção da fatia 5, mais adiante.

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

        requisicaoDeConsulta().queryParam("tipo", IMOBILIARIO).queryParam("estado", "EM_ANALISE").queryParam("tamanho", 100)
                .when().get("/processos").then().statusCode(200)
                .body("itens.tipo", everyItem(equalTo(IMOBILIARIO)))
                .body("itens.estado", everyItem(equalTo("EM_ANALISE")));
    }

    @Test
    @DisplayName("Listagem respeita página e tamanho e informa o total")
    void listagemPaginada() {
        abrirProcesso(novaCota(), SERVICO);

        requisicaoDeConsulta().queryParam("pagina", 1).queryParam("tamanho", 1).when().get("/processos").then().statusCode(200)
                .body("pagina", equalTo(1))
                .body("tamanho", equalTo(1))
                .body("itens.size()", lessThanOrEqualTo(1))
                .body("total", notNullValue());
    }

    // Suposição de erro.

    @Test
    @DisplayName("Corpo malformado é recusado com 400")
    void corpoMalformado() {
        requisicaoDeCliente().contentType(ContentType.JSON).body("{\"cotaId\": ").when().post("/processos").then().statusCode(400)
                .body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("Corpo vazio é recusado com 400")
    void corpoVazio() {
        requisicaoDeCliente().contentType(ContentType.JSON).when().post("/processos").then().statusCode(400)
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
        segurarPagamentoSolicitado(base);

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
        segurarPagamentoSolicitado(base);

        executarComObservacao(processo, base.acao(), observacao).statusCode(200)
                .body("estado", equalTo(base.destino()));

        Map<String, Object> ultimo = consultarHistoricoSemTentativas(processo).getLast();
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
    @ValueSource(strings = {PENDENTE_DOCUMENTO, REPROVADO, AGUARDANDO_CONCORDANCIA, PAGAMENTO_SOLICITADO,
            AGUARDANDO_ALCADA})
    @DisplayName("Listagem filtra pelas situações pendente de documento, reprovado, aguardando concordância, pagamento solicitado e aguardando alçada")
    void listagemFiltraPorSituacoesNovas(String situacao) {
        long processo = processoEmCotaNova(SERVICO, situacao);

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

    /** Cada processo pago custa a espera da rotina: o valor-limite usa uma amostra dos cenários (ver o modelo). */
    static Stream<CenarioCredito> cenariosComCotaLivre() {
        return ModeloPagamentoProcesso.cenariosComCotaLivreAmostrados();
    }

    static Stream<CenarioCredito> cenariosComCotaLivreECreditoRestante() {
        return ModeloPagamentoProcesso.cenariosComCotaLivre()
                .filter(c -> c.creditoDisponivelEsperado().signum() > 0);
    }

    static Stream<CenarioCredito> cenariosComCotaLivreAmostradosECreditoRestante() {
        return cenariosComCotaLivre().filter(c -> c.creditoDisponivelEsperado().signum() > 0);
    }

    /** Cota nova com o crédito do cenário e seus processos levados às situações do cenário. */
    private static long cotaDoCenario(CenarioCredito cenario) {
        long cota = novaCota(cenario.credito());
        for (ProcessoPlanejado planejado : cenario.processos()) {
            long processo = abrirProcesso(cota, planejado.tipo(), planejado.valor());
            levar(processo, planejado.caminho());
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
    @MethodSource("cenariosComCotaLivreAmostradosECreditoRestante")
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
        levar(pago, caminhoAte(SERVICO, PAGO, new BigDecimal("100000.00")));
        assertThat(consultarProcesso(pago).get("estado"), equalTo(PAGO));
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
        levar(pago, caminhoAte(SERVICO, PAGO));

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
        levar(pago, caminhoAte(SERVICO, PAGO, new BigDecimal("100000.00")));

        assertThat(consultarCreditoDisponivel(outra), comparesEqualTo(new BigDecimal("150000.00")));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));
        long processo = abrirProcesso(cota, SERVICO, CREDITO_PADRAO);
        assertThat(valorDoProcesso(consultarProcesso(processo)), comparesEqualTo(CREDITO_PADRAO));
    }

    @Test
    @DisplayName("Pagamento recusado pela tesouraria não consome crédito; pago depois, consome")
    void pagamentoNaoConcluidoNaoConsomeCredito() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        levar(processo, caminhoAte(SERVICO, APROVADO, new BigDecimal("100000.00")));

        TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
        aplicar(processo, SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(processo, APROVADO);

        assertThat(consultarProcesso(processo).get("estado"), equalTo(APROVADO));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));

        TesourariaStub.confirmar();
        aplicar(processo, SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(processo, PAGO);
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(new BigDecimal("150000.00")));
    }

    @Test
    @DisplayName("Crédito só diminui quando o processo é pago: aprovado e pagamento solicitado ainda não consomem")
    void creditoSoDiminuiNoPagamento() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO, new BigDecimal("100000.00"));
        levar(processo, caminhoAte(SERVICO, APROVADO, new BigDecimal("100000.00")));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));

        TesourariaStub.ficarIndisponivel();
        aplicar(processo, SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarTentativas(processo, 1);
        assertThat(consultarProcesso(processo).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(consultarCreditoDisponivel(cota), comparesEqualTo(CREDITO_PADRAO));

        TesourariaStub.confirmar();
        aguardarSituacao(processo, PAGO);

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

        levar(processo, caminhoAte(tipo, PAGO));

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
        // Aguardando alçada só existe com o processo acima da alçada (R16): os favorecidos somam o valor dele.
        CasoFavorecidos caso = caminho.faixa() == Faixa.ACIMA_DA_ALCADA
                ? ModeloPagamentoProcesso.favorecidosAcimaDaAlcada() : ModeloPagamentoProcesso.exemploDoCasoDeUso();
        long processo = abrirProcesso(novaCota(), caminho.tipo(), caso.valor(), caso.favorecidos());
        levar(processo, caminho);

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
        List<Map<String, Object>> historico = consultarHistoricoSemTentativas(processo);
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

        TesourariaStub.confirmar();
        passo(a, SOLICITAR_PAGAMENTO, PAGAMENTO_SOLICITADO);
        aguardarSituacao(a.processo(), PAGO);
        a.esperado().confirmar(ROTINA_CONFIRMOU);

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
        levar(pago, caminhoAte(SERVICO, PAGO, new BigDecimal("100000.00")));
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
        // Cada processo pago custa a espera da rotina: "dois pagos" fica de fora (já está em creditoDisponivelConsideraSoOsPagos).
        List<String> amostra = List.of("cota sem processo", "pago 100000 (serviço)",
                "crédito com centavos: pago 500,25 de 1000,50");
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
        levar(pago, caminhoAte(SERVICO, PAGO, new BigDecimal("100000.00")));
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


    // ---- Fatia 5: pagamento como saga (R7, R8, R20, R21, R22, R31) ----
    // Solicitar só grava; uma rotina agendada (a cada segundo no ambiente de teste) fala com a tesouraria. Os casos
    // esperam o efeito da rotina consultando a API em laço, e conferem o que a tesouraria RECEBEU pelo stub.

    static Stream<Arguments> respostasPorTipo() {
        return ModeloPagamentoProcesso.respostasPorTipo();
    }

    static Stream<Arguments> acoesDepoisDaRecusa() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, DEVOLVER_PARA_ANALISE, EM_ANALISE),
                Arguments.of(tipo, CANCELAR, CANCELADO)));
    }

    // R7: solicitar só grava, não depende da tesouraria. O endpoint antigo não existe mais.

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Solicitar pagamento com a tesouraria indisponível é aceito e o processo fica em pagamento solicitado, sem protocolo")
    void solicitarComTesourariaIndisponivelFicaEmPagamentoSolicitado(String tipo) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, APROVADO));
        TesourariaStub.ficarIndisponivel();

        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200).body("estado", equalTo(PAGAMENTO_SOLICITADO));

        // R8: a tesouraria indisponível não recusa nem paga: a rotina tenta e o processo continua onde está.
        aguardarTentativas(a.processo(), 1);
        Map<String, Object> processo = consultarProcesso(a.processo());
        assertThat(processo.get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(processo.get("protocoloPagamento"), nullValue());
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        List<Map<String, Object>> historico = consultarHistoricoSemTentativas(a.processo());
        Map<String, Object> ultimo = historico.getLast();
        assertThat(ultimo.get("acao"), equalTo("SOLICITAR_PAGAMENTO"));
        assertThat(ultimo.get("de"), equalTo(APROVADO));
        assertThat(ultimo.get("para"), equalTo(PAGAMENTO_SOLICITADO));
    }

    @Test
    @DisplayName("O endpoint antigo de pagar não existe mais: 404 e nada muda")
    void pagarNaoExisteMais() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        int itensNoHistorico = consultarHistorico(a.processo()).size();

        executar(a.processo(), PAGAR).statusCode(404);
        executar(INEXISTENTE, PAGAR).statusCode(404);

        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(APROVADO));
        assertThat(consultarHistorico(a.processo()).size(), equalTo(itensNoHistorico));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Em aprovado a consulta de ações oferece solicitar-pagamento no lugar de pagar")
    void aprovadoOfereceSolicitarPagamentoNoLugarDePagar(String tipo) {
        long processo = processoAprovado(tipo);

        List<String> acoes = consultarAcoes(processo);

        assertThat(acoes, hasItem(SOLICITAR_PAGAMENTO));
        assertThat(acoes, not(hasItem(PAGAR)));
    }

    // R20, R21, R22, R31: tabela de decisão da resposta da tesouraria × efeito no processo.

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("respostasPorTipo")
    @DisplayName("Cada resposta da tesouraria tem o efeito do modelo: situação, protocolo, histórico, crédito e cota")
    void respostaDaTesourariaTemOEfeitoDoModelo(Resposta resposta, String tipo) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, APROVADO));
        int antes = consultarHistorico(a.processo()).size();
        tesourariaResponde(resposta);

        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200).body("estado", equalTo(PAGAMENTO_SOLICITADO));
        if (resposta.aplicada()) {
            aguardarSituacao(a.processo(), resposta.situacaoDepois);
        } else {
            // R31 e R22: nada é aplicado e a rotina tenta de novo, com a mesma identificação.
            aguardarTentativas(a.processo(), 2);
            aguardarOrdens(a.processo(), 2);
            assertThat(identificacaoDaSolicitacao(a.processo()), not(blankOrNullString()));
        }

        Map<String, Object> processo = consultarProcesso(a.processo());
        assertThat(processo.get("estado"), equalTo(resposta.situacaoDepois));
        assertThat(processo.get("protocoloPagamento"), equalTo(resposta.protocolo()));
        BigDecimal credito = resposta.paga() ? CREDITO_PADRAO.subtract(VALOR_PADRAO) : CREDITO_PADRAO;
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(credito));

        List<String> esperadas = new ArrayList<>(List.of("SOLICITAR_PAGAMENTO"));
        if (resposta.aplicada()) {
            esperadas.add(resposta.acaoDoResultado);
        }
        List<Map<String, Object>> historico = consultarHistoricoSemTentativas(a.processo());
        assertThat(acoesDesde(a.processo(), antes), equalTo(esperadas));
        if (resposta == Resposta.RECUSOU) {
            assertThat(historico.getLast().get("observacao"), equalTo(MOTIVO_DA_TESOURARIA));
            assertThat(historico.getLast().get("de"), equalTo(PAGAMENTO_SOLICITADO));
            assertThat(historico.getLast().get("para"), equalTo(APROVADO));
        }
        if (!resposta.aplicada()) {
            conferirTentativas(a.processo(), 2);
        }

        // R6 e R20: só o pago libera a cota; ainda aprovado ou em pagamento solicitado, ela segue indisponível.
        if (resposta.paga()) {
            assertThat(consultarAcoes(a.processo()), empty());
            long novo = abrirProcesso(a.cota(), SERVICO);
            assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
        } else {
            tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409).body("mensagem", notNullValue());
            if (resposta == Resposta.RECUSOU) {
                assertThat(consultarAcoes(a.processo()), hasItem(SOLICITAR_PAGAMENTO));
            } else {
                assertThat(consultarAcoes(a.processo()), empty());
            }
        }
    }

    // R22: indisponível por algumas tentativas e depois confirma, sempre com a mesma identificação.

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Tesouraria indisponível por três tentativas e depois confirma: a identificação é a mesma e o processo vai a pago")
    void indisponivelPorAlgumasTentativasDepoisConfirma(String tipo) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, APROVADO));
        int antes = consultarHistorico(a.processo()).size();
        TesourariaStub.ficarIndisponivel();
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);

        aguardarTentativas(a.processo(), 3);
        aguardarOrdens(a.processo(), 3);
        String identificacao = identificacaoDaSolicitacao(a.processo());
        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        conferirTentativas(a.processo(), 3);

        TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
        aguardarSituacao(a.processo(), PAGO);

        Map<String, Object> processo = consultarProcesso(a.processo());
        assertThat(processo.get("estado"), equalTo(PAGO));
        assertThat(processo.get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(VALOR_PADRAO)));
        assertThat(identificacaoDaSolicitacao(a.processo()), equalTo(identificacao));
        assertThat(acoesColapsadasDesde(a.processo(), antes),
                contains("SOLICITAR_PAGAMENTO", TENTAR_PAGAMENTO, "CONFIRMAR_PAGAMENTO"));
        assertThat(quantidadeDeTentativas(a.processo()), greaterThanOrEqualTo(3L));
    }

    @Test
    @DisplayName("Tesouraria demora mais de 2 segundos nas primeiras tentativas e depois diz que já fez a ordem: vale como confirmação")
    void semRespostaDepoisJaProcessouValeComoConfirmacao() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        TesourariaStub.demorar();
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarTentativas(a.processo(), 2);
        aguardarOrdens(a.processo(), 2);
        String identificacao = identificacaoDaSolicitacao(a.processo());
        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));

        TesourariaStub.jaProcessou(PROTOCOLO_DA_TESOURARIA);
        aguardarSituacao(a.processo(), PAGO);

        assertThat(consultarProcesso(a.processo()).get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
        assertThat(identificacaoDaSolicitacao(a.processo()), equalTo(identificacao));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(VALOR_PADRAO)));
    }

    @Test
    @DisplayName("Tesouraria que responde dentro do prazo (1 segundo de atraso) tem a resposta aplicada")
    void respostaDentroDoPrazoEAplicada() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        TesourariaStub.confirmarComAtraso(Duration.ofSeconds(1), PROTOCOLO_DA_TESOURARIA);

        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(a.processo(), PAGO);

        assertThat(consultarProcesso(a.processo()).get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
    }

    // R31: resposta incoerente não é aplicada; a nova tentativa, coerente, é.

    @Test
    @DisplayName("Confirmação sem protocolo não é aplicada; na tentativa seguinte a confirmação com protocolo leva a pago")
    void confirmacaoSemProtocoloNaoEAplicadaEDepoisConfirma() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        int antes = consultarHistorico(a.processo()).size();
        TesourariaStub.confirmarSemProtocolo();
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);

        aguardarTentativas(a.processo(), 2);
        aguardarOrdens(a.processo(), 2);
        String identificacao = identificacaoDaSolicitacao(a.processo());
        Map<String, Object> processo = consultarProcesso(a.processo());
        assertThat(processo.get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(processo.get("protocoloPagamento"), nullValue());
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        conferirTentativas(a.processo(), 2);

        TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
        aguardarSituacao(a.processo(), PAGO);

        assertThat(consultarProcesso(a.processo()).get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
        assertThat(identificacaoDaSolicitacao(a.processo()), equalTo(identificacao));
        assertThat(acoesColapsadasDesde(a.processo(), antes),
                contains("SOLICITAR_PAGAMENTO", TENTAR_PAGAMENTO, "CONFIRMAR_PAGAMENTO"));
    }

    @Test
    @DisplayName("Recusa sem motivo não é aplicada; na tentativa seguinte a recusa com motivo volta a aprovado com o motivo no histórico")
    void recusaSemMotivoNaoEAplicadaEDepoisRecusaComMotivo() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        int antes = consultarHistorico(a.processo()).size();
        TesourariaStub.recusarSemMotivo();
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);

        aguardarTentativas(a.processo(), 2);
        aguardarOrdens(a.processo(), 2);
        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(acoesDesde(a.processo(), antes), contains("SOLICITAR_PAGAMENTO"));
        conferirTentativas(a.processo(), 2);

        TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
        aguardarSituacao(a.processo(), APROVADO);

        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(APROVADO));
        assertThat(consultarHistoricoSemTentativas(a.processo()).getLast().get("observacao"),
                equalTo(MOTIVO_DA_TESOURARIA));
        assertThat(acoesColapsadasDesde(a.processo(), antes),
                contains("SOLICITAR_PAGAMENTO", TENTAR_PAGAMENTO, "RECUSAR_PAGAMENTO"));
    }

    // R21: recusa volta a aprovado; nova solicitação, sem limite de vezes, com identificação nova.

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Recusada duas vezes e confirmada na terceira: cada solicitação tem identificação nova e o motivo de cada recusa fica no histórico")
    void recusaVoltaAAprovadoENovaSolicitacaoTemIdentificacaoNova(String tipo) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, APROVADO));
        int antes = consultarHistorico(a.processo()).size();

        for (int rodada = 1; rodada <= 2; rodada++) {
            String motivo = MOTIVO_DA_TESOURARIA + " " + rodada;
            TesourariaStub.recusar(motivo);
            executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200).body("estado", equalTo(PAGAMENTO_SOLICITADO));
            aguardarSituacao(a.processo(), APROVADO);

            Map<String, Object> processo = consultarProcesso(a.processo());
            assertThat(processo.get("estado"), equalTo(APROVADO));
            assertThat(processo.get("protocoloPagamento"), nullValue());
            assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
            assertThat(consultarAcoes(a.processo()), hasItem(SOLICITAR_PAGAMENTO));
            tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409);
            Map<String, Object> recusa = consultarHistoricoSemTentativas(a.processo()).getLast();
            assertThat(recusa.get("acao"), equalTo("RECUSAR_PAGAMENTO"));
            assertThat(recusa.get("de"), equalTo(PAGAMENTO_SOLICITADO));
            assertThat(recusa.get("para"), equalTo(APROVADO));
            assertThat(recusa.get("observacao"), equalTo(motivo));
            assertThat(TesourariaStub.identificacoesRecebidas(a.processo()).size(), equalTo(rodada));
        }

        TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(a.processo(), PAGO);

        List<String> identificacoes = TesourariaStub.identificacoesRecebidas(a.processo());
        assertThat(identificacoes.size(), equalTo(3));
        assertThat(identificacoes.stream().distinct().count(), equalTo(3L));
        assertThat(identificacoes, everyItem(not(blankOrNullString())));
        assertThat(consultarProcesso(a.processo()).get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(VALOR_PADRAO)));
        assertThat(acoesDesde(a.processo(), antes),
                contains("SOLICITAR_PAGAMENTO", "RECUSAR_PAGAMENTO", "SOLICITAR_PAGAMENTO", "RECUSAR_PAGAMENTO",
                        "SOLICITAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("acoesDepoisDaRecusa")
    @DisplayName("Depois da recusa da tesouraria o processo aprovado pode ser devolvido para análise ou cancelado, como qualquer aprovado")
    void recusaDaTesourariaNaoImpedeDevolverNemCancelar(String tipo, String acao, String destino) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, APROVADO));
        TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(a.processo(), APROVADO);

        executarComMotivo(a.processo(), acao, "depois da recusa da tesouraria").statusCode(200)
                .body("estado", equalTo(destino));

        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(destino));
        assertThat(consultarHistoricoSemTentativas(a.processo()).getLast().get("observacao"),
                equalTo("depois da recusa da tesouraria"));
        if (CANCELADO.equals(destino)) {
            long novo = abrirProcesso(a.cota(), SERVICO);
            assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
        }
    }

    // R20: em pagamento solicitado toda ação de pessoa é recusada; a cota segue indisponível.

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Em pagamento solicitado cancelar e devolver para análise são recusados com 409, a cota segue indisponível e nada muda")
    void cancelarEDevolverEmPagamentoSolicitadoSaoRecusados(String tipo) {
        Acompanhado a = aberto(tipo);
        levar(a.processo(), caminhoAte(tipo, PAGAMENTO_SOLICITADO));
        int itensNoHistorico = consultarHistoricoSemTentativas(a.processo()).size();

        executarComMotivo(a.processo(), CANCELAR, "desistência da cota").statusCode(409)
                .body("mensagem", notNullValue());
        executarComMotivo(a.processo(), DEVOLVER_PARA_ANALISE, "documento divergente").statusCode(409)
                .body("mensagem", notNullValue());

        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(consultarHistoricoSemTentativas(a.processo()).size(), equalTo(itensNoHistorico));
        tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409);
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
    }

    @Test
    @DisplayName("Listagem por pagamento solicitado acompanha a rotina: o processo sai da lista quando é pago")
    void listagemAcompanhaARotina() {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, PAGAMENTO_SOLICITADO));
        assertThat(idsDaListagem(PAGAMENTO_SOLICITADO), hasItem(a.processo()));
        assertThat(idsDaListagem(APROVADO), not(hasItem(a.processo())));

        TesourariaStub.confirmar();
        aguardarSituacao(a.processo(), PAGO);

        assertThat(idsDaListagem(PAGAMENTO_SOLICITADO), not(hasItem(a.processo())));
        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(PAGO));
    }

    // R20: o que a tesouraria recebe: identificação, processo, valor e partes vigentes.

    @Test
    @DisplayName("O pedido à tesouraria leva o valor e as partes vigentes depois de alterações do cliente em aberto")
    void pedidoTemOsDadosVigentesDepoisDeAlterarEmAberto() {
        Acompanhado a = aberto(SERVICO);
        passo(a, ALTERAR, ABERTO);
        passo(a, ALTERAR, ABERTO);
        passo(a, INICIAR_ANALISE, EM_ANALISE);
        passo(a, CONCLUIR_ANALISE, APROVADO);

        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(a.processo(), PAGO);

        conferirPedidos(a.processo(), a.esperado());
    }

    @Test
    @DisplayName("O pedido à tesouraria leva o valor e as partes que o cliente aceitou na proposta do backoffice")
    void pedidoTemOsDadosVigentesDepoisDeConcordarComAProposta() {
        Acompanhado a = emAnalise(IMOBILIARIO);
        passo(a, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA);
        passo(a, CONCORDAR, EM_ANALISE);
        passo(a, CONCLUIR_ANALISE, "AGUARDANDO_VISTORIA");
        passo(a, CONCLUIR_VISTORIA, APROVADO);

        executar(a.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(a.processo(), PAGO);

        conferirPedidos(a.processo(), a.esperado());
    }

    /** Todo pedido que a tesouraria recebeu do processo tem a identificação, o valor e as partes vigentes (R15, R20). */
    private static void conferirPedidos(long processo, ProcessoEsperado esperado) {
        List<OrdemRecebida> ordens = TesourariaStub.ordensRecebidas(processo);
        assertThat(ordens, not(empty()));
        for (OrdemRecebida ordem : ordens) {
            assertThat(ordem.requestId(), not(blankOrNullString()));
            assertThat(ordem.processId(), equalTo(processo));
            assertThat(ordem.valor(), comparesEqualTo(esperado.valor()));
            assertThat(ordem.partes().size(), equalTo(esperado.favorecidos().size()));
            for (Map<String, Object> favorecido : esperado.favorecidos()) {
                Parte parte = ordem.partes().stream()
                        .filter(p -> p.documento().equals(favorecido.get("documento"))).findFirst()
                        .orElseThrow(() -> new AssertionError("Sem a parte do favorecido " + favorecido));
                assertThat(parte.nome(), equalTo(favorecido.get("nome")));
                assertThat(parte.papel(), equalTo(favorecido.get("papel")));
                assertThat(parte.valor(), comparesEqualTo((BigDecimal) favorecido.get("valor")));
            }
        }
    }

    // Dois processos de cotas diferentes em pagamento solicitado ao mesmo tempo.

    @Test
    @DisplayName("Duas cotas em pagamento solicitado ao mesmo tempo: uma recusada e outra confirmada, cada uma com seu resultado")
    void duasCotasEmPagamentoSolicitadoCadaUmaComSeuResultado() {
        Acompanhado recusada = aberto(SERVICO);
        Acompanhado confirmada = aberto(IMOBILIARIO);
        levar(recusada.processo(), caminhoAte(SERVICO, APROVADO));
        levar(confirmada.processo(), caminhoAte(IMOBILIARIO, APROVADO));
        TesourariaStub.confirmar();
        TesourariaStub.recusarProcesso(recusada.processo(), "conta inválida da primeira");
        TesourariaStub.confirmarProcesso(confirmada.processo(), "PROTOCOLO-DA-SEGUNDA");

        executar(recusada.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        executar(confirmada.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(recusada.processo(), APROVADO);
        aguardarSituacao(confirmada.processo(), PAGO);

        Map<String, Object> daRecusada = consultarProcesso(recusada.processo());
        assertThat(daRecusada.get("estado"), equalTo(APROVADO));
        assertThat(daRecusada.get("protocoloPagamento"), nullValue());
        assertThat(consultarCreditoDisponivel(recusada.cota()), comparesEqualTo(CREDITO_PADRAO));
        assertThat(consultarHistoricoSemTentativas(recusada.processo()).getLast().get("observacao"),
                equalTo("conta inválida da primeira"));
        Map<String, Object> daConfirmada = consultarProcesso(confirmada.processo());
        assertThat(daConfirmada.get("estado"), equalTo(PAGO));
        assertThat(daConfirmada.get("protocoloPagamento"), equalTo("PROTOCOLO-DA-SEGUNDA"));
        assertThat(consultarCreditoDisponivel(confirmada.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(VALOR_PADRAO)));
        assertThat(identificacaoDaSolicitacao(recusada.processo()),
                not(equalTo(identificacaoDaSolicitacao(confirmada.processo()))));
    }

    @Test
    @DisplayName("Duas cotas em pagamento solicitado com resposta incoerente da tesouraria: cada uma repete só a própria identificação, e as duas são diferentes")
    void duasSolicitacoesPendentesTemIdentificacoesDiferentes() {
        Acompanhado primeira = aberto(SERVICO);
        Acompanhado segunda = aberto(SERVICO);
        levar(primeira.processo(), caminhoAte(SERVICO, APROVADO));
        levar(segunda.processo(), caminhoAte(SERVICO, APROVADO));
        // Resposta incoerente (R31), e não erro: com a tesouraria fora do ar o circuito de proteção deixa passar poucos pedidos.
        TesourariaStub.confirmarSemProtocolo();

        executar(primeira.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        executar(segunda.processo(), SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarOrdens(primeira.processo(), 2);
        aguardarOrdens(segunda.processo(), 2);

        assertThat(identificacaoDaSolicitacao(primeira.processo()),
                not(equalTo(identificacaoDaSolicitacao(segunda.processo()))));
        assertThat(consultarProcesso(primeira.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
        assertThat(consultarProcesso(segunda.processo()).get("estado"), equalTo(PAGAMENTO_SOLICITADO));
    }

    // R20: a rotina só olha processos em pagamento solicitado.

    @Test
    @DisplayName("A rotina só pede à tesouraria os processos em pagamento solicitado: pago, aprovado, em análise e recusado não geram novos pedidos")
    void rotinaSoOlhaProcessosEmPagamentoSolicitado() {
        long pago = processoEmCotaNova(SERVICO, PAGO);
        long aprovado = processoEmCotaNova(SERVICO, APROVADO);
        long emAnalise = processoEmCotaNova(SERVICO, EM_ANALISE);
        long recusado = processoEmCotaNova(SERVICO, APROVADO);
        TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
        executar(recusado, SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarSituacao(recusado, APROVADO);
        int ordensDoPago = TesourariaStub.quantidadeDeOrdens(pago);
        int ordensDoRecusado = TesourariaStub.quantidadeDeOrdens(recusado);
        assertThat(ordensDoPago, greaterThanOrEqualTo(1));
        assertThat(ordensDoRecusado, greaterThanOrEqualTo(1));

        // Um processo em pagamento solicitado serve de relógio: três pedidos dele são três rodadas da rotina.
        TesourariaStub.confirmarSemProtocolo();
        long relogio = processoEmCotaNova(SERVICO, APROVADO);
        executar(relogio, SOLICITAR_PAGAMENTO).statusCode(200);
        aguardarOrdens(relogio, 3);

        assertThat(TesourariaStub.quantidadeDeOrdens(aprovado), equalTo(0));
        assertThat(TesourariaStub.quantidadeDeOrdens(emAnalise), equalTo(0));
        assertThat(TesourariaStub.quantidadeDeOrdens(pago), equalTo(ordensDoPago));
        assertThat(TesourariaStub.quantidadeDeOrdens(recusado), equalTo(ordensDoRecusado));
    }

    // Recusas: dois pedidos de solicitar pagamento ao mesmo tempo, quem chega primeiro vale.

    @Test
    @DisplayName("Solicitações de pagamento simultâneas: uma vale, as outras são recusadas e a tesouraria recebe uma só identificação")
    void solicitacoesSimultaneasDePagamentoValemUmaSo() throws Exception {
        Acompanhado a = aberto(SERVICO);
        levar(a.processo(), caminhoAte(SERVICO, APROVADO));
        int antes = consultarHistorico(a.processo()).size();
        int quantidade = 4;
        CyclicBarrier largada = new CyclicBarrier(quantidade);
        Callable<Integer> pedido = () -> {
            largada.await(30, TimeUnit.SECONDS);
            return executar(a.processo(), SOLICITAR_PAGAMENTO).extract().statusCode();
        };

        List<Integer> respostas = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(quantidade)) {
            for (Future<Integer> resposta : executor.invokeAll(Collections.nCopies(quantidade, pedido))) {
                respostas.add(resposta.get());
            }
        }
        aguardarSituacao(a.processo(), PAGO);

        assertThat(Collections.frequency(respostas, 200), equalTo(1));
        assertThat(Collections.frequency(respostas, 409), equalTo(quantidade - 1));
        assertThat(acoesDesde(a.processo(), antes),
                contains("SOLICITAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"));
        assertThat(TesourariaStub.identificacoesRecebidas(a.processo()).size(), equalTo(1));
    }

    // ---- Fatia 6: alçada (R16, com R2, R3, R4, R6, R11, R13, R23) ----
    // Concluir a análise (serviço) ou a vistoria (imobiliário) decide, com o valor VIGENTE, entre aprovado (até a alçada,
    // inclusive) e aguardando alçada. Oráculo: ModeloPagamentoProcesso (faixas de valor e decisaoDaAlcada).
    // A tabela situação × ação (linha aguardando alçada e coluna aprovar na alçada) e os caminhos estão nos testes
    // acaoForaDaOrdemERecusada, acoesAceitasNaSituacao, fluxoPelaAlcadaChegaASituacaoFinal e nos de motivo e observação.
    // R17 e R18 (quem aprova, papéis): em PapeisEAutoriaAceitacaoTest (fatia 7). Aqui o auxiliar envia o token do papel certo.

    private static final List<String> ACOES_EM_AGUARDANDO_ALCADA =
            List.of("aprovar-na-alcada", "reprovar", "devolver-para-analise", "cancelar");

    /** Abre com o valor dado e um favorecido que o recebe, na cota padrão, com o oráculo dos dados vigentes. */
    private static Acompanhado abertoComValor(String tipo, BigDecimal valor) {
        long cota = novaCota();
        ProcessoEsperado esperado = ProcessoEsperado.comValor(valor);
        long processo = abrirProcesso(cota, tipo, esperado.valor(), esperado.favorecidos());
        return new Acompanhado(cota, processo, esperado, tipo);
    }

    private static Acompanhado emAnaliseComValor(String tipo, BigDecimal valor) {
        Acompanhado a = abertoComValor(tipo, valor);
        passo(a, INICIAR_ANALISE, EM_ANALISE);
        return a;
    }

    /**
     * Conclui o que decide a alçada e confere a situação a que o processo foi (R16): no serviço, concluir a análise; no
     * imobiliário, concluir a análise (que sempre leva à vistoria, R3) e depois concluir a vistoria.
     */
    private static void concluirEConferirDecisao(Acompanhado a, String esperada) {
        if (IMOBILIARIO.equals(a.tipo())) {
            passo(a, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA);
            passo(a, CONCLUIR_VISTORIA, esperada);
        } else {
            passo(a, CONCLUIR_ANALISE, esperada);
        }
    }

    /** Processo acima da alçada que acabou de ser concluído: aguardando alçada. */
    private static Acompanhado aguardandoAlcada(String tipo) {
        Acompanhado a = emAnaliseComValor(tipo, VALOR_ACIMA_DA_ALCADA);
        concluirEConferirDecisao(a, AGUARDANDO_ALCADA);
        return a;
    }

    // Valor-limite da alçada (R16): 49.999,99 e 50.000,00 aprovam; 50.000,01 e acima vão a aguardando alçada.

    static Stream<Arguments> valoresDaAlcada() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "um centavo abaixo da alçada: 49.999,99", new BigDecimal("49999.99"), APROVADO),
                Arguments.of(tipo, "exatamente a alçada: 50.000,00", new BigDecimal("50000.00"), APROVADO),
                Arguments.of(tipo, "um centavo acima da alçada: 50.000,01", new BigDecimal("50000.01"),
                        AGUARDANDO_ALCADA),
                Arguments.of(tipo, "bem acima da alçada: 100.000,00", new BigDecimal("100000.00"), AGUARDANDO_ALCADA),
                Arguments.of(tipo, "o crédito inteiro da cota: 250.000,00", new BigDecimal("250000.00"),
                        AGUARDANDO_ALCADA)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("valoresDaAlcada")
    @DisplayName("Concluir decide pela alçada: até 50.000,00, inclusive, vai a aprovado; acima vai a aguardando alçada")
    void concluirDecidePelaAlcada(String tipo, String descricao, BigDecimal valor, String esperada) {
        assertThat("o oráculo do modelo concorda com a regra", decisaoDaAlcada(valor), equalTo(esperada));
        Acompanhado a = emAnaliseComValor(tipo, valor);
        String antesDeDecidir = EM_ANALISE;
        if (IMOBILIARIO.equals(tipo)) {
            // R3: no imobiliário a análise sempre leva à vistoria, qualquer que seja o valor; é a vistoria que decide.
            passo(a, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA);
            conferir(a, AGUARDANDO_VISTORIA);
            antesDeDecidir = AGUARDANDO_VISTORIA;
            passo(a, CONCLUIR_VISTORIA, esperada);
        } else {
            passo(a, CONCLUIR_ANALISE, esperada);
        }

        conferir(a, esperada);
        Map<String, Object> ultimo = consultarHistorico(a.processo()).getLast();
        assertThat(ultimo.get("de"), equalTo(antesDeDecidir));
        assertThat(ultimo.get("para"), equalTo(esperada));
        assertThat(ultimo.get("acao"), equalTo(IMOBILIARIO.equals(tipo) ? "CONCLUIR_VISTORIA" : "CONCLUIR_ANALISE"));
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(valor));
        // R6 e R13: aprovado ou aguardando alçada, a cota segue indisponível e o crédito não é consumido.
        tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409).body("mensagem", notNullValue());
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        if (APROVADO.equals(esperada)) {
            assertThat(consultarAcoes(a.processo()), hasItem(SOLICITAR_PAGAMENTO));
            assertThat(consultarAcoes(a.processo()), not(hasItem(APROVAR_NA_ALCADA)));
        } else {
            assertThat(consultarAcoes(a.processo()), containsInAnyOrder(ACOES_EM_AGUARDANDO_ALCADA.toArray()));
        }
    }

    // Aguardando alçada: o que vale, o que a cota e o crédito fazem (R6, R13, R4, R11, R23).

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Em aguardando alçada a consulta de ações lista exatamente aprovar na alçada, reprovar, devolver para análise e cancelar")
    void aguardandoAlcadaOfereceQuatroAcoes(String tipo) {
        Acompanhado a = aguardandoAlcada(tipo);

        assertThat(consultarAcoes(a.processo()), containsInAnyOrder("aprovar-na-alcada", "reprovar",
                "devolver-para-analise", "cancelar"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Em aguardando alçada a cota fica indisponível, o crédito não é consumido e a listagem filtra a situação")
    void aguardandoAlcadaMantemACotaIndisponivelESemConsumirCredito(String tipo) {
        Acompanhado a = aguardandoAlcada(tipo);

        tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409).body("mensagem", notNullValue());
        assertThat(quantidadeDeProcessosDaCota(a.cota()), equalTo(1L));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        Map<String, Object> item = consultarProcesso(a.processo());
        assertThat(item.get("estado"), equalTo(AGUARDANDO_ALCADA));
        assertThat(valorDoProcesso(item), comparesEqualTo(VALOR_ACIMA_DA_ALCADA));
        assertThat(item.get("protocoloPagamento"), nullValue());
        assertThat(idsDaListagem(AGUARDANDO_ALCADA), hasItem(a.processo()));
        assertThat(idsDaListagem(APROVADO), not(hasItem(a.processo())));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Aprovar na alçada leva a aprovado, e o processo segue por solicitar pagamento até pago, consumindo o crédito só no pago")
    void aprovarNaAlcadaSeguePeloPagamentoAtePago(String tipo) {
        Acompanhado a = aguardandoAlcada(tipo);
        int antes = consultarHistorico(a.processo()).size();

        passo(a, APROVAR_NA_ALCADA, APROVADO);

        conferir(a, APROVADO);
        assertThat(consultarAcoes(a.processo()), hasItem(SOLICITAR_PAGAMENTO));
        assertThat(consultarAcoes(a.processo()), not(hasItem(APROVAR_NA_ALCADA)));
        assertThat(idsDaListagem(AGUARDANDO_ALCADA), not(hasItem(a.processo())));
        assertThat(idsDaListagem(APROVADO), hasItem(a.processo()));
        TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
        passo(a, SOLICITAR_PAGAMENTO, PAGAMENTO_SOLICITADO);
        aguardarSituacao(a.processo(), PAGO);
        a.esperado().confirmar(ROTINA_CONFIRMOU);

        conferir(a, PAGO);
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO.subtract(VALOR_ACIMA_DA_ALCADA)));
        assertThat(consultarProcesso(a.processo()).get("protocoloPagamento"), equalTo(PROTOCOLO_DA_TESOURARIA));
        assertThat(acoesDesde(a.processo(), antes),
                contains("APROVAR_NA_ALCADA", "SOLICITAR_PAGAMENTO", "CONFIRMAR_PAGAMENTO"));
        Map<String, Object> aprovacao = consultarHistorico(a.processo()).get(antes);
        assertThat(aprovacao.get("de"), equalTo(AGUARDANDO_ALCADA));
        assertThat(aprovacao.get("para"), equalTo(APROVADO));
        long novo = abrirProcesso(a.cota(), SERVICO);
        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
    }

    static Stream<Arguments> saidasFinaisDaAlcada() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, REPROVAR, REPROVADO, "favorecido divergente"),
                Arguments.of(tipo, CANCELAR, CANCELADO, "desistência da cota")));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("saidasFinaisDaAlcada")
    @DisplayName("Reprovar ou cancelar em aguardando alçada, com motivo, é final: não consome crédito e libera a cota")
    void reprovarOuCancelarEmAguardandoAlcadaEFinalELiberaACota(String tipo, String acao, String situacaoFinal,
            String motivo) {
        Acompanhado a = aguardandoAlcada(tipo);
        int antes = consultarHistorico(a.processo()).size();

        executarComMotivo(a.processo(), acao, motivo).statusCode(200).body("estado", equalTo(situacaoFinal));
        a.esperado().confirmar(acao);

        conferir(a, situacaoFinal);
        List<Map<String, Object>> novas = consultarHistorico(a.processo()).stream().skip(antes).toList();
        assertThat(novas.size(), equalTo(1));
        assertThat(novas.getFirst().get("de"), equalTo(AGUARDANDO_ALCADA));
        assertThat(novas.getFirst().get("para"), equalTo(situacaoFinal));
        assertThat(novas.getFirst().get("acao"), equalTo(nomeNoHistorico(acao)));
        assertThat(novas.getFirst().get("observacao"), equalTo(motivo));
        assertThat(consultarAcoes(a.processo()), empty());
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
        // R14: a cota está livre e o crédito inteiro disponível.
        long novo = abrirProcesso(a.cota(), SERVICO, CREDITO_PADRAO);
        assertThat(consultarProcesso(novo).get("estado"), equalTo(ABERTO));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Devolver para análise em aguardando alçada, com motivo, volta a em análise e a cota segue indisponível")
    void devolverEmAguardandoAlcadaVoltaParaAnalise(String tipo) {
        Acompanhado a = aguardandoAlcada(tipo);
        int antes = consultarHistorico(a.processo()).size();

        executarComMotivo(a.processo(), DEVOLVER_PARA_ANALISE, "documento divergente").statusCode(200)
                .body("estado", equalTo(EM_ANALISE));
        a.esperado().confirmar(DEVOLVER_PARA_ANALISE);

        conferir(a, EM_ANALISE);
        List<Map<String, Object>> novas = consultarHistorico(a.processo()).stream().skip(antes).toList();
        assertThat(novas.size(), equalTo(1));
        assertThat(novas.getFirst().get("de"), equalTo(AGUARDANDO_ALCADA));
        assertThat(novas.getFirst().get("para"), equalTo(EM_ANALISE));
        assertThat(novas.getFirst().get("acao"), equalTo("DEVOLVER_PARA_ANALISE"));
        assertThat(novas.getFirst().get("observacao"), equalTo("documento divergente"));
        assertThat(consultarAcoes(a.processo()),
                containsInAnyOrder(ModeloPagamentoProcesso.acoesAceitas(tipo, EM_ANALISE).toArray()));
        tentarAbrirProcesso(a.cota(), SERVICO).statusCode(409);
    }

    // Depois de devolver e concluir de novo, a alçada é decidida de novo, com o valor vigente daquele momento.

    @ParameterizedTest(name = "{0}")
    @MethodSource("tipos")
    @DisplayName("Devolver de aguardando alçada e concluir de novo, sem mudar o valor, volta a aguardando alçada (no imobiliário, repete a vistoria)")
    void devolverEConcluirDeNovoSemMudarOValorDecideDeNovo(String tipo) {
        Acompanhado a = aguardandoAlcada(tipo);
        int antes = consultarHistorico(a.processo()).size();

        passo(a, DEVOLVER_PARA_ANALISE, EM_ANALISE);
        concluirEConferirDecisao(a, AGUARDANDO_ALCADA);

        conferir(a, AGUARDANDO_ALCADA);
        List<String> esperadas = IMOBILIARIO.equals(tipo)
                ? List.of("DEVOLVER_PARA_ANALISE", "CONCLUIR_ANALISE", "CONCLUIR_VISTORIA")
                : List.of("DEVOLVER_PARA_ANALISE", "CONCLUIR_ANALISE");
        assertThat(acoesDesde(a.processo(), antes), equalTo(esperadas));
        assertThat(consultarAcoes(a.processo()), containsInAnyOrder(ACOES_EM_AGUARDANDO_ALCADA.toArray()));
    }

    static Stream<Arguments> devolucoesQueMudamOValor() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "abre com 60.000, vai a aguardando alçada; devolvido e proposto 40.000, passa a aprovado",
                        new BigDecimal("60000.00"), AGUARDANDO_ALCADA, new BigDecimal("40000.00"), APROVADO),
                Arguments.of(tipo, "abre com 40.000, vai a aprovado; devolvido e proposto 60.000, passa a aguardando alçada",
                        new BigDecimal("40000.00"), APROVADO, new BigDecimal("60000.00"), AGUARDANDO_ALCADA)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("devolucoesQueMudamOValor")
    @DisplayName("Devolver para análise, mudar o valor com a concordância do cliente e concluir de novo decide a alçada com o valor novo")
    void devolverEMudarOValorRefazADecisaoDaAlcada(String tipo, String descricao, BigDecimal inicial, String primeira,
            BigDecimal proposto, String segunda) {
        Acompanhado a = emAnaliseComValor(tipo, inicial);
        concluirEConferirDecisao(a, primeira);
        passo(a, DEVOLVER_PARA_ANALISE, EM_ANALISE);
        Alteracao proposta = new Alteracao(proposto, favorecidoUnico(proposto));
        tentarProporAlteracao(a.processo(), proposta.valor(), proposta.favorecidos(), "valor corrigido").statusCode(200)
                .body("estado", equalTo(AGUARDANDO_CONCORDANCIA));
        a.esperado().propor(proposta, "valor corrigido");
        passo(a, CONCORDAR, EM_ANALISE);

        concluirEConferirDecisao(a, segunda);

        conferir(a, segunda);
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(proposto));
        assertThat(consultarCreditoDisponivel(a.cota()), comparesEqualTo(CREDITO_PADRAO));
    }

    // Alçada com o valor vigente (R16, D35): vale o valor de depois de qualquer alteração.

    static Stream<Arguments> alteracoesDoClienteEmAberto() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "abre com 60.000, o cliente altera para 40.000", new BigDecimal("60000.00"),
                        new BigDecimal("40000.00"), APROVADO),
                Arguments.of(tipo, "abre com 40.000, o cliente altera para 60.000", new BigDecimal("40000.00"),
                        new BigDecimal("60000.00"), AGUARDANDO_ALCADA),
                Arguments.of(tipo, "abre com 60.000, o cliente altera para exatamente 50.000,00", new BigDecimal("60000.00"),
                        new BigDecimal("50000.00"), APROVADO),
                Arguments.of(tipo, "abre com 40.000, o cliente altera para 50.000,01", new BigDecimal("40000.00"),
                        new BigDecimal("50000.01"), AGUARDANDO_ALCADA)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("alteracoesDoClienteEmAberto")
    @DisplayName("A alteração do cliente em aberto muda a decisão da alçada: vale o valor vigente ao concluir, não o da abertura")
    void alteracaoEmAbertoMudaADecisaoDaAlcada(String tipo, String descricao, BigDecimal inicial, BigDecimal alterado,
            String esperada) {
        Acompanhado a = abertoComValor(tipo, inicial);
        Alteracao nova = new Alteracao(alterado, favorecidoUnico(alterado));
        tentarAlterar(a.processo(), nova.valor(), nova.favorecidos()).statusCode(200).body("estado", equalTo(ABERTO));
        a.esperado().alterar(nova);
        passo(a, INICIAR_ANALISE, EM_ANALISE);

        concluirEConferirDecisao(a, esperada);

        conferir(a, esperada);
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(alterado));
    }

    static Stream<Arguments> propostasDoBackoffice() {
        return tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "abre com 40.000, o backoffice propõe 60.000 e o cliente concorda",
                        new BigDecimal("40000.00"), new BigDecimal("60000.00"), CONCORDAR, AGUARDANDO_ALCADA),
                Arguments.of(tipo, "abre com 40.000, o backoffice propõe 60.000 e o cliente discorda",
                        new BigDecimal("40000.00"), new BigDecimal("60000.00"), DISCORDAR, APROVADO),
                Arguments.of(tipo, "abre com 60.000, o backoffice propõe 40.000 e o cliente concorda",
                        new BigDecimal("60000.00"), new BigDecimal("40000.00"), CONCORDAR, APROVADO),
                Arguments.of(tipo, "abre com 60.000, o backoffice propõe 40.000 e o cliente discorda",
                        new BigDecimal("60000.00"), new BigDecimal("40000.00"), DISCORDAR, AGUARDANDO_ALCADA)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("propostasDoBackoffice")
    @DisplayName("A proposta do backoffice só muda a decisão da alçada se o cliente concorda; se discorda, vale o valor anterior")
    void propostaSoMudaADecisaoDaAlcadaSeOClienteConcorda(String tipo, String descricao, BigDecimal inicial,
            BigDecimal proposto, String resposta, String esperada) {
        Acompanhado a = emAnaliseComValor(tipo, inicial);
        Alteracao proposta = new Alteracao(proposto, favorecidoUnico(proposto));
        tentarProporAlteracao(a.processo(), proposta.valor(), proposta.favorecidos(), "valor corrigido").statusCode(200)
                .body("estado", equalTo(AGUARDANDO_CONCORDANCIA));
        a.esperado().propor(proposta, "valor corrigido");
        passo(a, resposta, EM_ANALISE);

        concluirEConferirDecisao(a, esperada);

        conferir(a, esperada);
        BigDecimal vigente = CONCORDAR.equals(resposta) ? proposto : inicial;
        assertThat(valorDoProcesso(consultarProcesso(a.processo())), comparesEqualTo(vigente));
    }

    // Efeito observável nos outros endpoints: a listagem acompanha a aprovação na alçada.

    @Test
    @DisplayName("Listagem por aguardando alçada acompanha a aprovação: o processo sai dela e entra em aprovado")
    void listagemAcompanhaAAprovacaoNaAlcada() {
        Acompanhado a = aguardandoAlcada(SERVICO);
        assertThat(idsDaListagem(AGUARDANDO_ALCADA), hasItem(a.processo()));
        assertThat(idsDaListagem(APROVADO), not(hasItem(a.processo())));

        passo(a, APROVAR_NA_ALCADA, APROVADO);

        assertThat(idsDaListagem(AGUARDANDO_ALCADA), not(hasItem(a.processo())));
        assertThat(idsDaListagem(APROVADO), hasItem(a.processo()));
    }

    // Recusas: aprovar e reprovar ao mesmo tempo, quem chega primeiro vale e o outro é tratado como fora da ordem.

    @Test
    @DisplayName("Pedidos simultâneos de aprovar na alçada e reprovar: um só vale, e a situação e o histórico são coerentes com ele")
    void aprovarEReprovarNaAlcadaAoMesmoTempoValeUmSo() throws Exception {
        Acompanhado a = aguardandoAlcada(SERVICO);
        int antes = consultarHistorico(a.processo()).size();
        int quantidade = 4;
        CyclicBarrier largada = new CyclicBarrier(quantidade);
        List<Callable<Integer>> pedidos = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            boolean aprova = i % 2 == 0;
            pedidos.add(() -> {
                largada.await(30, TimeUnit.SECONDS);
                return (aprova ? executar(a.processo(), APROVAR_NA_ALCADA)
                        : executarComMotivo(a.processo(), REPROVAR, "favorecido divergente")).extract().statusCode();
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
        List<Map<String, Object>> novas = consultarHistorico(a.processo()).stream().skip(antes).toList();
        assertThat(novas.size(), equalTo(1));
        String vencedora = (String) novas.getFirst().get("acao");
        String situacao = "APROVAR_NA_ALCADA".equals(vencedora) ? APROVADO : REPROVADO;
        assertThat(vencedora, org.hamcrest.Matchers.anyOf(equalTo("APROVAR_NA_ALCADA"), equalTo("REPROVAR")));
        assertThat(novas.getFirst().get("de"), equalTo(AGUARDANDO_ALCADA));
        assertThat(novas.getFirst().get("para"), equalTo(situacao));
        assertThat(consultarProcesso(a.processo()).get("estado"), equalTo(situacao));
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

    /** Processo de uma cota nova, aberto com o valor da faixa do caminho (R16) e levado por ele. */
    private static long processoEm(Caminho caminho) {
        long processo = abrirProcesso(novaCota(), caminho.tipo(), caminho.valor());
        levar(processo, caminho);
        return processo;
    }

    private static long processoEm(Base base) {
        return processoEm(caminhoAte(base.tipo(), base.faixa(), base.situacao()));
    }

    private static long processoAprovado() {
        long processo = abrirProcesso(novaCota(), SERVICO);
        executar(processo, INICIAR_ANALISE).statusCode(200);
        executar(processo, CONCLUIR_ANALISE).statusCode(200).body("estado", equalTo(APROVADO));
        return processo;
    }

    private static long processoAprovado(String tipo) {
        return processoEmCotaNova(tipo, APROVADO);
    }

    /** Processo de uma cota nova, levado pelo caminho mais curto até a situação (aguardando alçada: acima da alçada). */
    private static long processoEmCotaNova(String tipo, String situacao) {
        return processoEm(caminhoAte(tipo, situacao));
    }

    /**
     * Leva o processo, já aberto, pelo caminho do modelo. A rotina é do ambiente: ao solicitar o pagamento, a
     * tesouraria já fica como o próximo passo do caminho manda (confirma, recusa) ou, se o caminho termina em
     * pagamento solicitado, fora do ar, para a rotina não tirar o processo dali. "Rotina confirmou" e "rotina
     * recusou" são esperas pelo efeito da rotina.
     */
    private static void levar(long processo, Caminho caminho) {
        List<Passo> passos = caminho.passos();
        for (int i = 0; i < passos.size(); i++) {
            Passo passo = passos.get(i);
            switch (passo.acao()) {
                case ROTINA_CONFIRMOU -> {
                    TesourariaStub.confirmar();
                    aguardarSituacao(processo, PAGO);
                }
                case ROTINA_RECUSOU -> {
                    TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
                    aguardarSituacao(processo, APROVADO);
                    TesourariaStub.confirmar();
                }
                case SOLICITAR_PAGAMENTO -> {
                    String proxima = i + 1 < passos.size() ? passos.get(i + 1).acao() : null;
                    if (ROTINA_CONFIRMOU.equals(proxima)) {
                        TesourariaStub.confirmar();
                    } else if (ROTINA_RECUSOU.equals(proxima)) {
                        TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
                    } else {
                        TesourariaStub.ficarIndisponivel();
                    }
                    aplicar(processo, passo.acao()).statusCode(200).body("estado", equalTo(passo.situacao()));
                }
                default -> aplicar(processo, passo.acao()).statusCode(200).body("estado", equalTo(passo.situacao()));
            }
        }
    }

    /** Solicitar pagamento leva a pagamento solicitado, e a tesouraria fora do ar o mantém ali para ser conferido. */
    private static void segurarPagamentoSolicitado(Base base) {
        if (SOLICITAR_PAGAMENTO.equals(base.acao())) {
            TesourariaStub.ficarIndisponivel();
        }
    }

    private static void tesourariaResponde(Resposta resposta) {
        switch (resposta) {
            case CONFIRMOU -> TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
            case JA_PROCESSOU -> TesourariaStub.jaProcessou(PROTOCOLO_DA_TESOURARIA);
            case RECUSOU -> TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
            case INDISPONIVEL -> TesourariaStub.ficarIndisponivel();
            case DEMOROU -> TesourariaStub.demorar();
            case CONFIRMACAO_SEM_PROTOCOLO -> TesourariaStub.confirmarSemProtocolo();
            case RECUSA_SEM_MOTIVO -> TesourariaStub.recusarSemMotivo();
        }
    }

    /** Espera a tesouraria ter recebido ao menos {@code minimo} pedidos do processo. */
    private static void aguardarOrdens(long processo, int minimo) {
        aguardar("a tesouraria tivesse recebido %d pedidos do processo %d".formatted(minimo, processo),
                () -> TesourariaStub.quantidadeDeOrdens(processo), quantidade -> quantidade >= minimo);
    }

    /** A identificação da solicitação: a mesma em todos os pedidos do processo (R22), e não vazia. */
    private static String identificacaoDaSolicitacao(long processo) {
        List<String> identificacoes = TesourariaStub.identificacoesRecebidas(processo);
        assertThat("uma só identificação nos pedidos do processo " + processo, identificacoes.size(), equalTo(1));
        assertThat(identificacoes.getFirst(), not(blankOrNullString()));
        return identificacoes.getFirst();
    }

    /** Ações do histórico depois das primeiras {@code antes} entradas (as do caminho até aprovado), sem as tentativas. */
    private static List<String> acoesDesde(long processo, int antes) {
        return consultarHistorico(processo).stream().skip(antes)
                .filter(item -> !TENTAR_PAGAMENTO.equals(item.get("acao"))).map(item -> (String) item.get("acao")).toList();
    }

    /** Como {@link #acoesColapsadas}, só do que veio depois das primeiras {@code antes} entradas. */
    private static List<String> acoesColapsadasDesde(long processo, int antes) {
        List<String> acoes = new ArrayList<>();
        for (Map<String, Object> item : consultarHistorico(processo).stream().skip(antes).toList()) {
            String acao = (String) item.get("acao");
            if (TENTAR_PAGAMENTO.equals(acao) && !acoes.isEmpty() && TENTAR_PAGAMENTO.equals(acoes.getLast())) {
                continue;
            }
            acoes.add(acao);
        }
        return acoes;
    }

    /** Histórico em ordem, com as tentativas seguidas da rotina reduzidas a uma (o número delas não é certo). */
    private static List<String> acoesColapsadas(long processo) {
        List<String> acoes = new ArrayList<>();
        for (Map<String, Object> item : consultarHistorico(processo)) {
            String acao = (String) item.get("acao");
            if (TENTAR_PAGAMENTO.equals(acao) && !acoes.isEmpty() && TENTAR_PAGAMENTO.equals(acoes.getLast())) {
                continue;
            }
            acoes.add(acao);
        }
        return acoes;
    }

    /** R19, R22: ao menos {@code minimo} tentativas no histórico, cada uma em pagamento solicitado e com o fato descrito. */
    private static void conferirTentativas(long processo, int minimo) {
        List<Map<String, Object>> tentativas = consultarHistorico(processo).stream()
                .filter(item -> TENTAR_PAGAMENTO.equals(item.get("acao"))).toList();
        assertThat(tentativas.size(), greaterThanOrEqualTo(minimo));
        for (Map<String, Object> tentativa : tentativas) {
            assertThat(tentativa.get("de"), equalTo(PAGAMENTO_SOLICITADO));
            assertThat(tentativa.get("para"), equalTo(PAGAMENTO_SOLICITADO));
            assertThat((String) tentativa.get("observacao"), not(blankOrNullString()));
        }
    }
}
