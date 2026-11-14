package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.VALOR_PADRAO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.abrirProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.aguardarSituacao;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.aguardarTentativas;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.clienteDaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarAcoes;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarComo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarCreditoDisponivel;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarFavorecidos;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarHistorico;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarHistoricoSemTentativas;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.consultarProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.corpoAbertura;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.enviarComo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executar;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComCorpo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.executarComo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.favorecidoUnico;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.json;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.novaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.quantidadeDeProcessosDaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.sucesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarAbrirProcesso;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarAbrirProcessoComo;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.tentarConsultarProposta;
import static br.com.claudio.consorcio.aceitacao.IdentidadeTeste.SEM_TOKEN;
import static br.com.claudio.consorcio.aceitacao.IdentidadeTeste.SISTEMA;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste;
import br.com.claudio.consorcio.aceitacao.IdentidadeTeste.Pessoa;
import br.com.claudio.consorcio.aceitacao.TesourariaStub;
import br.com.claudio.consorcio.aceitacao.TesourariaStubResource;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Caminho;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.Passo;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.ProcessoEsperado;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPapeis.Celula;
import br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPapeis.Credencial;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.response.ValidatableResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Padrão: Teste de aceitação baseado em modelos [Ammann e Offutt; Myers].
 * Fatia 7 do caso de uso de pagamento de processo: autoria e papéis (R17, R18 e o autor de R19). Roda na fase
 * unitária e, por herança em {@link PapeisEAutoriaAceitacaoIT}, contra o artefato empacotado.
 *
 * <p>Modelos: a tabela papel × ação ({@link ModeloPapeis}); a tabela de decisão de R17 (papel de aprovador × ser a
 * pessoa da última conclusão da análise); o histórico com o autor em caminhos completos do modelo de estados
 * ({@link ModeloPagamentoProcesso}). Os 401 e os 403 são conferidos pelo status, pela mensagem (quando há corpo) e
 * por "nada mudou": situação (histórico), ações oferecidas, favorecidos, proposta e crédito.
 */
@QuarkusTest
@WithTestResource(TesourariaStubResource.class)
class PapeisEAutoriaAceitacaoTest {

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

    // ---- Auxiliares ----

    private record Preparado(long cota, long processo) {
    }

    /** Tudo o que o negócio enxerga de um processo e da cota, para provar que uma recusa não mudou nada. */
    private record Retrato(List<Map<String, Object>> historico, List<String> acoes,
            List<Map<String, Object>> favorecidos, String proposta, BigDecimal credito) {
    }

    private static Retrato retrato(Preparado p) {
        return new Retrato(consultarHistorico(p.processo()), consultarAcoes(p.processo()),
                consultarFavorecidos(p.processo()), tentarConsultarProposta(p.processo()).extract().asString(),
                consultarCreditoDisponivel(p.cota()));
    }

    /** Pedido da ação, com o corpo que o contrato exige (motivo, valor e favorecidos válidos), com a identidade dada. */
    private static ValidatableResponse pedir(String autorizacao, long processo, String acao) {
        Map<String, Object> corpo = ProcessoEsperado.inicial().preparar(acao);
        return executarComo(autorizacao, processo, acao, corpo == null ? null : json(corpo));
    }

    private static ValidatableResponse como(Pessoa pessoa, long processo, String acao) {
        return pedir(pessoa.autorizacao(), processo, acao);
    }

    /** Recusa por identidade: status certo e, quando há corpo, a mensagem do formato de erro de sempre. */
    private static void conferirRecusa(ValidatableResponse resposta, int status) {
        resposta.statusCode(status);
        if (!resposta.extract().asString().isBlank()) {
            resposta.body("mensagem", notNullValue());
        }
    }

    /** Processo de uma cota nova, aberto com o valor dado e levado pelo caminho mais curto até a situação. */
    private static Preparado processoEm(String tipo, String situacao, BigDecimal valor) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, tipo, valor);
        ProcessoEsperado esperado = ProcessoEsperado.inicial();
        for (Passo passo : caminhoCacheado(tipo, situacao, valor).passos()) {
            Map<String, Object> corpo = esperado.preparar(passo.acao());
            ValidatableResponse resposta = corpo == null ? executar(processo, passo.acao())
                    : executarComCorpo(processo, passo.acao(), json(corpo));
            resposta.statusCode(200).body("estado", equalTo(passo.situacao()));
            esperado.confirmar(passo.acao());
        }
        return new Preparado(cota, processo);
    }

    /** O caminho do modelo é uma busca em profundidade; o resultado é sempre o mesmo, então fica guardado. */
    private static final Map<String, Caminho> CAMINHOS = new ConcurrentHashMap<>();

    private static Caminho caminhoCacheado(String tipo, String situacao, BigDecimal valor) {
        return CAMINHOS.computeIfAbsent(tipo + "|" + situacao + "|" + faixaDoValor(valor),
                chave -> caminhoAte(tipo, situacao, valor));
    }

    private static String situacaoDe(long processo) {
        List<Map<String, Object>> historico = consultarHistorico(processo);
        return historico.isEmpty() ? ABERTO : (String) historico.getLast().get("para");
    }

    /** Entradas do histórico depois das primeiras {@code antes} (a comparação é só com o que veio depois). */
    private static List<Map<String, Object>> entradasDesde(long processo, int antes) {
        return consultarHistorico(processo).stream().skip(antes).toList();
    }

    // ---- R18: tabela papel × ação ----

    static Stream<Arguments> papelPorAcao() {
        return ModeloPapeis.celulas().flatMap(celula -> Stream.of(Credencial.values())
                .map(credencial -> Arguments.of(celula, credencial)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("papelPorAcao")
    @DisplayName("R18: a ação só é aceita de quem tem o papel dela; os outros, 403 (ou 401 sem token), e nada muda")
    void acaoSoEAceitaDeQuemTemOPapel(Celula celula, Credencial credencial) {
        Preparado p = processoEm(celula.tipo(), celula.situacao(), celula.valor());
        Pessoa quem = credencial.pessoa(p.cota());
        String autorizacao = quem == null ? SEM_TOKEN : quem.autorizacao();
        if (SOLICITAR_PAGAMENTO.equals(celula.acao())) {
            // A tesouraria fora do ar mantém o processo em pagamento solicitado, para a rotina não o mudar.
            TesourariaStub.ficarIndisponivel();
        }
        Retrato antes = retrato(p);

        ValidatableResponse resposta = pedir(autorizacao, p.processo(), celula.acao());

        if (celula.aceita(credencial)) {
            resposta.statusCode(200).body("estado", equalTo(celula.destino()));
            List<Map<String, Object>> novas = entradasDesde(p.processo(), antes.historico().size());
            assertThat("entradas novas no histórico", novas, not(empty()));
            Map<String, Object> entrada = novas.getFirst();
            assertThat(entrada.get("acao"), equalTo(nomeNoHistorico(celula.acao())));
            assertThat(entrada.get("de"), equalTo(celula.situacao()));
            assertThat(entrada.get("para"), equalTo(celula.destino()));
            assertThat("autor da ação (R19)", entrada.get("autor"), equalTo(quem.usuario()));
            if (!SOLICITAR_PAGAMENTO.equals(celula.acao())) {
                assertThat(novas.size(), equalTo(1));
            }
        } else {
            conferirRecusa(resposta, credencial.statusDaRecusa());
            assertThat("nada muda depois da recusa", retrato(p), equalTo(antes));
            assertThat(situacaoDe(p.processo()), equalTo(celula.situacao()));
        }
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { SERVICO, IMOBILIARIO })
    @DisplayName("R18: quem é dono da cota é o cliente do token (claim clienteId), não o nome do usuário")
    void donoEPeloClienteIdDoToken(String tipo) {
        Preparado p = processoEm(tipo, ABERTO, VALOR_PADRAO);
        long dono = clienteDaCota(p.cota()).clienteId();
        long outro = clienteDaCota(novaCota()).clienteId();
        Retrato antes = retrato(p);

        // O usuário parece o do dono, mas o cadastro do token é de outro cliente.
        Pessoa impostor = new Pessoa("cliente-" + dono, Set.of(IdentidadeTeste.CLIENTE), outro);
        conferirRecusa(pedir(impostor.autorizacao(), p.processo(), ALTERAR), 403);
        assertThat(retrato(p), equalTo(antes));

        // O usuário é outro, mas o cadastro do token é o do dono.
        Pessoa maria = new Pessoa("maria", Set.of(IdentidadeTeste.CLIENTE), dono);
        pedir(maria.autorizacao(), p.processo(), ALTERAR).statusCode(200).body("estado", equalTo(ABERTO));
        assertThat(entradasDesde(p.processo(), 0).getFirst().get("autor"), equalTo("maria"));
    }

    // ---- R18: abrir processo, só o cliente da cota ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(Credencial.class)
    @DisplayName("R1 e R18: só o cliente da cota abre o processo; os outros, 403 (ou 401), e nada é criado")
    void aberturaSoEAceitaDoClienteDaCota(Credencial credencial) {
        long cota = novaCota();
        Pessoa quem = credencial.pessoa(cota);
        String autorizacao = quem == null ? SEM_TOKEN : quem.autorizacao();

        ValidatableResponse resposta = tentarAbrirProcessoComo(autorizacao, cota, SERVICO);

        if (credencial == Credencial.DONO) {
            long processo = resposta.statusCode(201).extract().jsonPath().getLong("id");
            assertThat(consultarProcesso(processo).get("estado"), equalTo(ABERTO));
            assertThat(quantidadeDeProcessosDaCota(cota), equalTo(1L));
        } else {
            conferirRecusa(resposta, credencial.statusDaRecusa());
            assertThat(quantidadeDeProcessosDaCota(cota), equalTo(0L));
            // A recusa não prendeu a cota: o dono abre em seguida.
            abrirProcesso(cota, SERVICO);
        }
    }

    // ---- R18: cadastros (decisão do coordenador: papel backoffice) ----

    enum Cadastro {
        CLIENTE("/clientes"), COTA("/cotas");

        final String caminho;

        Cadastro(String caminho) {
            this.caminho = caminho;
        }
    }

    static Stream<Arguments> cadastrosPorCredencial() {
        return Stream.of(Cadastro.values()).flatMap(cadastro -> Stream.of(Credencial.BACKOFFICE, Credencial.DONO,
                Credencial.APROVADOR, Credencial.TESOURARIA, Credencial.SEM_PAPEL, Credencial.SEM_TOKEN)
                .map(credencial -> Arguments.of(cadastro, credencial)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("cadastrosPorCredencial")
    @DisplayName("Decisão do coordenador: cadastro de cliente e de cota só do backoffice")
    void cadastroSoEAceitoDoBackoffice(Cadastro cadastro, Credencial credencial) {
        long cotaDeReferencia = novaCota();
        Pessoa quem = credencial.pessoa(cotaDeReferencia);
        String autorizacao = quem == null ? SEM_TOKEN : quem.autorizacao();
        long unico = System.nanoTime();
        Object corpo = switch (cadastro) {
            case CLIENTE -> Map.of("nome", "Cadastro de papel", "cpf", "%011d".formatted(unico % 100_000_000_000L));
            case COTA -> Map.of("numero", "COTA-PAPEL-" + unico, "valorCredito", new BigDecimal("1000.00"),
                    "clienteId", clienteDaCota(cotaDeReferencia).clienteId());
        };

        ValidatableResponse resposta = enviarComo(autorizacao, cadastro.caminho, corpo);

        if (credencial == Credencial.BACKOFFICE) {
            resposta.statusCode(sucesso());
        } else {
            conferirRecusa(resposta, credencial.statusDaRecusa());
        }
    }

    // ---- R18: consultas, todos os papéis ----

    enum Consulta {
        LISTA("/processos?tamanho=1", false),
        ACOES("/processos/%d/acoes", true),
        HISTORICO("/processos/%d/historico", true),
        FAVORECIDOS("/processos/%d/favorecidos", true),
        PROPOSTA("/processos/%d/alteracao-proposta", true),
        COTA("/cotas/%d", false);

        final String caminho;
        /** Consulta de detalhe do processo: o caso de uso a cita para o cliente da cota (D37), ver dúvida F7-3. */
        final boolean detalheDoProcesso;

        Consulta(String caminho, boolean detalheDoProcesso) {
            this.caminho = caminho;
            this.detalheDoProcesso = detalheDoProcesso;
        }

        String caminho(Preparado p) {
            return this == COTA ? caminho.formatted(p.cota()) : caminho.contains("%d") ? caminho.formatted(p.processo())
                    : caminho;
        }
    }

    static Stream<Arguments> consultasPorCredencial() {
        return Stream.of(Consulta.values()).flatMap(consulta -> Stream.of(Credencial.DONO, Credencial.BACKOFFICE,
                Credencial.APROVADOR, Credencial.TESOURARIA, Credencial.OUTRO_CLIENTE, Credencial.SEM_TOKEN)
                // Cliente de outra cota em consulta de detalhe do processo: dúvida F7-3, não testado.
                .filter(credencial -> credencial != Credencial.OUTRO_CLIENTE || !consulta.detalheDoProcesso)
                .map(credencial -> Arguments.of(consulta, credencial)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("consultasPorCredencial")
    @DisplayName("R18: consultar é de todos os papéis; sem token, 401")
    void consultaEDeTodosOsPapeis(Consulta consulta, Credencial credencial) {
        // Aguardando concordância: a proposta existe, e a consulta dela responde 200.
        Preparado p = processoEm(SERVICO, AGUARDANDO_CONCORDANCIA, VALOR_PADRAO);
        Pessoa quem = credencial.pessoa(p.cota());
        String autorizacao = quem == null ? SEM_TOKEN : quem.autorizacao();

        ValidatableResponse resposta = consultarComo(autorizacao, consulta.caminho(p));

        if (credencial == Credencial.SEM_TOKEN) {
            conferirRecusa(resposta, 401);
        } else {
            resposta.statusCode(200);
        }
    }

    // ---- 401: identificação ausente ou inválida ----

    enum Defeito {
        SEM_TOKEN, ASSINATURA_DE_OUTRA_CHAVE, EXPIRADO, EMISSOR_DIFERENTE, TEXTO_QUE_NAO_E_TOKEN;

        String autorizacao(Pessoa pessoa) {
            return switch (this) {
                case SEM_TOKEN -> IdentidadeTeste.SEM_TOKEN;
                case ASSINATURA_DE_OUTRA_CHAVE -> IdentidadeTeste.autorizacaoAssinadaPorOutraChave(pessoa);
                case EXPIRADO -> IdentidadeTeste.autorizacaoExpirada(pessoa);
                case EMISSOR_DIFERENTE -> IdentidadeTeste.autorizacaoDeOutroEmissor(pessoa);
                case TEXTO_QUE_NAO_E_TOKEN -> IdentidadeTeste.autorizacaoMalformada();
            };
        }
    }

    enum Operacao {
        LISTAR_PROCESSOS, CONSULTAR_COTA, INICIAR_ANALISE, ABRIR_PROCESSO, CADASTRAR_CLIENTE
    }

    static Stream<Arguments> defeitosPorOperacao() {
        return Stream.of(Defeito.values()).flatMap(defeito -> Stream.of(Operacao.values())
                .map(operacao -> Arguments.of(defeito, operacao)));
    }

    @ParameterizedTest(name = "{0} | {1}")
    @MethodSource("defeitosPorOperacao")
    @DisplayName("Sem token, token de outra chave, expirado, de outro emissor ou que não é token: 401, e nada muda")
    void identificacaoAusenteOuInvalidaE401(Defeito defeito, Operacao operacao) {
        long cota = novaCota();
        Pessoa dono = clienteDaCota(cota);
        switch (operacao) {
            case LISTAR_PROCESSOS -> conferirRecusa(
                    consultarComo(defeito.autorizacao(IdentidadeTeste.ANA), "/processos"), 401);
            case CONSULTAR_COTA -> conferirRecusa(
                    consultarComo(defeito.autorizacao(IdentidadeTeste.ANA), "/cotas/%d".formatted(cota)), 401);
            case INICIAR_ANALISE -> {
                long processo = abrirProcesso(cota, SERVICO);
                Retrato antes = retrato(new Preparado(cota, processo));
                conferirRecusa(executarComo(defeito.autorizacao(IdentidadeTeste.ANA), processo, INICIAR_ANALISE, null),
                        401);
                assertThat(retrato(new Preparado(cota, processo)), equalTo(antes));
                // A recusa não travou o processo: com um token bom a ação vale.
                executar(processo, INICIAR_ANALISE).statusCode(200).body("estado", equalTo(EM_ANALISE));
            }
            case ABRIR_PROCESSO -> {
                conferirRecusa(tentarAbrirProcessoComo(defeito.autorizacao(dono), cota, SERVICO), 401);
                assertThat(quantidadeDeProcessosDaCota(cota), equalTo(0L));
            }
            case CADASTRAR_CLIENTE -> conferirRecusa(enviarComo(defeito.autorizacao(IdentidadeTeste.ANA), "/clientes",
                    Map.of("nome", "Sem identificação", "cpf", "%011d".formatted(System.nanoTime() % 100_000_000_000L))),
                    401);
        }
    }

    @Test
    @DisplayName("Contrato aberto: /q/openapi responde sem token")
    void openapiContinuaAberto() {
        consultarComo(SEM_TOKEN, "/q/openapi").statusCode(200);
    }

    // ---- Ordem das recusas: 401, depois papel (403), depois formato, processo e situação ----

    @ParameterizedTest(name = "{0}")
    @EnumSource(Defeito.class)
    @DisplayName("Ordem: a identificação vem antes de 404 (processo inexistente), 400 (corpo) e 409 (fora da ordem)")
    void identificacaoVemAntesDeTudo(Defeito defeito) {
        Preparado aberto = processoEm(SERVICO, ABERTO, VALOR_PADRAO);
        String autorizacao = defeito.autorizacao(IdentidadeTeste.ANA);

        conferirRecusa(executarComo(autorizacao, INEXISTENTE, INICIAR_ANALISE, null), 401);
        conferirRecusa(executarComo(autorizacao, aberto.processo(), SOLICITAR_DOCUMENTO, null), 401);
        conferirRecusa(executarComo(autorizacao, aberto.processo(), SOLICITAR_DOCUMENTO, "{\"motivo\": "), 401);
        conferirRecusa(executarComo(autorizacao, aberto.processo(), CONCLUIR_ANALISE, null), 401);
        conferirRecusa(enviarComo(autorizacao, "/processos", "{}"), 401);
        assertThat(consultarHistorico(aberto.processo()), empty());
    }

    @Test
    @DisplayName("Ordem: papel errado é 403 também para processo inexistente (antes do 404)")
    void papelErradoVemAntesDoProcessoInexistente() {
        conferirRecusa(executarComo(IdentidadeTeste.TEREZA.autorizacao(), INEXISTENTE, INICIAR_ANALISE, null), 403);
        conferirRecusa(executarComo(IdentidadeTeste.BRUNO.autorizacao(), INEXISTENTE, CONCLUIR_ANALISE, null), 403);
        conferirRecusa(pedir(IdentidadeTeste.ANA.autorizacao(), INEXISTENTE, CONCORDAR), 403);
    }

    @Test
    @DisplayName("Ordem: papel errado é 403 antes do 400 do corpo (motivo ausente, JSON malformado, valor inválido)")
    void papelErradoVemAntesDoFormatoDoCorpo() {
        Preparado emAnalise = processoEm(SERVICO, EM_ANALISE, VALOR_PADRAO);
        Preparado aberto = processoEm(SERVICO, ABERTO, VALOR_PADRAO);

        conferirRecusa(executarComo(IdentidadeTeste.TEREZA.autorizacao(), emAnalise.processo(), SOLICITAR_DOCUMENTO,
                null), 403);
        conferirRecusa(executarComo(IdentidadeTeste.TEREZA.autorizacao(), emAnalise.processo(), SOLICITAR_DOCUMENTO,
                "{\"motivo\": "), 403);
        conferirRecusa(executarComo(IdentidadeTeste.ANA.autorizacao(), aberto.processo(), ALTERAR, "{}"), 403);
        conferirRecusa(executarComo(clienteDaCota(emAnalise.cota()).autorizacao(), emAnalise.processo(), CANCELAR,
                null), 403);
        conferirRecusa(enviarComo(IdentidadeTeste.ANA.autorizacao(), "/processos",
                corpoAbertura(aberto.cota(), SERVICO, BigDecimal.ZERO, favorecidoUnico(VALOR_PADRAO))), 403);
        assertThat(consultarHistorico(aberto.processo()), empty());
    }

    @Test
    @DisplayName("Ordem: papel errado é 403 antes do 409 (ação fora da ordem) e antes do 404 e do 409 na abertura")
    void papelErradoVemAntesDaSituacaoEDosDadosDaAbertura() {
        Preparado aberto = processoEm(SERVICO, ABERTO, VALOR_PADRAO);
        Preparado emAnalise = processoEm(SERVICO, EM_ANALISE, VALOR_PADRAO);

        conferirRecusa(pedir(IdentidadeTeste.TEREZA.autorizacao(), emAnalise.processo(), INICIAR_ANALISE), 403);
        conferirRecusa(pedir(IdentidadeTeste.BRUNO.autorizacao(), aberto.processo(), SOLICITAR_PAGAMENTO), 403);
        conferirRecusa(pedir(IdentidadeTeste.ANA.autorizacao(), aberto.processo(), CONCORDAR), 403);

        // Abrir: backoffice com cota inexistente (404 sem o papel) e aprovador com cota em andamento (409 sem o papel).
        conferirRecusa(tentarAbrirProcessoComo(IdentidadeTeste.ANA.autorizacao(), INEXISTENTE, SERVICO), 403);
        conferirRecusa(tentarAbrirProcessoComo(IdentidadeTeste.BRUNO.autorizacao(), aberto.cota(), SERVICO), 403);
        assertThat(quantidadeDeProcessosDaCota(aberto.cota()), equalTo(1L));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { ALTERAR, CONCORDAR, DISCORDAR })
    @DisplayName("Ordem: cliente de outra cota em processo inexistente é 404 (o 404 vem antes do 403 de cota)")
    void processoInexistenteVemAntesDoClienteDeOutraCota(String acao) {
        String outroCliente = clienteDaCota(novaCota()).autorizacao();

        pedir(outroCliente, INEXISTENTE, acao).statusCode(404).body("mensagem", notNullValue());
    }

    @Test
    @DisplayName("R18: usuário válido sem nenhum papel não executa ação de escrita (403)")
    void tokenSemPapelNaoExecutaAcaoDeEscrita() {
        Preparado aberto = processoEm(SERVICO, ABERTO, VALOR_PADRAO);
        Retrato antes = retrato(aberto);

        conferirRecusa(pedir(IdentidadeTeste.VISITANTE.autorizacao(), aberto.processo(), INICIAR_ANALISE), 403);
        conferirRecusa(pedir(IdentidadeTeste.VISITANTE.autorizacao(), aberto.processo(), CANCELAR), 403);
        conferirRecusa(tentarAbrirProcessoComo(IdentidadeTeste.VISITANTE.autorizacao(), novaCota(), SERVICO), 403);
        assertThat(retrato(aberto), equalTo(antes));
    }

    // ---- R18: uma pessoa pode ter mais de um papel ----

    @Test
    @DisplayName("R18: pessoa com os papéis de backoffice e de aprovador exerce os dois, e o histórico guarda o usuário")
    void pessoaComDoisPapeisExerceOsDois() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO, VALOR_ACIMA_DA_ALCADA);

        como(IdentidadeTeste.DORA, processo, INICIAR_ANALISE).statusCode(200).body("estado", equalTo(EM_ANALISE));
        como(IdentidadeTeste.DORA, processo, CONCLUIR_ANALISE).statusCode(200)
                .body("estado", equalTo(AGUARDANDO_ALCADA));
        // Aprovador, ela não aprova o que concluiu (R17); outra pessoa de dois papéis aprova.
        como(IdentidadeTeste.CARLA, processo, APROVAR_NA_ALCADA).statusCode(200).body("estado", equalTo(APROVADO));

        assertThat(autoresDoHistorico(processo), contains("dora", "dora", "carla"));
    }

    @Test
    @DisplayName("R18: cliente da cota que também é backoffice abre o processo e inicia a análise, com o mesmo usuário")
    void clienteComPapelDeBackofficeExerceOsDois() {
        long cota = novaCota();
        long clienteId = clienteDaCota(cota).clienteId();
        Pessoa lia = new Pessoa("lia", Set.of(IdentidadeTeste.CLIENTE, IdentidadeTeste.BACKOFFICE), clienteId);

        long processo = tentarAbrirProcessoComo(lia.autorizacao(), cota, SERVICO).statusCode(201).extract()
                .jsonPath().getLong("id");
        pedir(lia.autorizacao(), processo, INICIAR_ANALISE).statusCode(200).body("estado", equalTo(EM_ANALISE));

        assertThat(autoresDoHistorico(processo), contains("lia"));
    }

    private static List<String> autoresDoHistorico(long processo) {
        return consultarHistoricoSemTentativas(processo).stream().map(item -> (String) item.get("autor")).toList();
    }

    // ---- R17: quem aprova não pode ser quem concluiu a análise ----

    /**
     * Processo acima da alçada em aguardando alçada. {@code iniciou} começa a análise, {@code concluiuAnalise} a conclui
     * e, no imobiliário, {@code concluiuVistoria} conclui a vistoria (que é quem leva o processo a aguardando alçada).
     */
    private static Preparado emAlcada(String tipo, Pessoa iniciou, Pessoa concluiuAnalise, Pessoa concluiuVistoria) {
        long cota = novaCota();
        long processo = abrirProcesso(cota, tipo, VALOR_ACIMA_DA_ALCADA);
        como(iniciou, processo, INICIAR_ANALISE).statusCode(200).body("estado", equalTo(EM_ANALISE));
        concluirAteAlcada(tipo, processo, concluiuAnalise, concluiuVistoria);
        return new Preparado(cota, processo);
    }

    private static void concluirAteAlcada(String tipo, long processo, Pessoa concluiuAnalise, Pessoa concluiuVistoria) {
        if (IMOBILIARIO.equals(tipo)) {
            como(concluiuAnalise, processo, CONCLUIR_ANALISE).statusCode(200)
                    .body("estado", equalTo(AGUARDANDO_VISTORIA));
            como(concluiuVistoria, processo, CONCLUIR_VISTORIA).statusCode(200)
                    .body("estado", equalTo(AGUARDANDO_ALCADA));
        } else {
            como(concluiuAnalise, processo, CONCLUIR_ANALISE).statusCode(200)
                    .body("estado", equalTo(AGUARDANDO_ALCADA));
        }
    }

    /** Linhas da tabela de decisão: (papel de aprovador) × (é a pessoa da última conclusão da análise). */
    static Stream<Arguments> quemTentaAprovar() {
        return ModeloPagamentoProcesso.tipos().flatMap(tipo -> Stream.of(
                Arguments.of(tipo, "aprovador que não concluiu", IdentidadeTeste.ANA, IdentidadeTeste.BRUNO, 200),
                Arguments.of(tipo, "aprovador e backoffice, não concluiu", IdentidadeTeste.ANA, IdentidadeTeste.DORA,
                        200),
                Arguments.of(tipo, "aprovador e backoffice que concluiu a análise", IdentidadeTeste.DORA,
                        IdentidadeTeste.DORA, 403),
                Arguments.of(tipo, "só backoffice, que concluiu a análise", IdentidadeTeste.ANA, IdentidadeTeste.ANA,
                        403),
                Arguments.of(tipo, "só backoffice, que não concluiu a análise", IdentidadeTeste.ANA, IdentidadeTeste.ARI,
                        403)));
    }

    @ParameterizedTest(name = "{0}: {1}")
    @MethodSource("quemTentaAprovar")
    @DisplayName("R17: aprova na alçada o aprovador que não concluiu a análise; quem concluiu, ou quem não é aprovador, 403")
    void soAprovaQuemTemOPapelENaoConcluiuAAnalise(String tipo, String linha, Pessoa concluiuAnalise, Pessoa tenta,
            int esperado) {
        // No imobiliário a vistoria é de outra pessoa (backoffice), para a análise ser o único vínculo de quem tenta.
        Preparado p = emAlcada(tipo, IdentidadeTeste.ANA, concluiuAnalise, IdentidadeTeste.ARI);
        Retrato antes = retrato(p);

        ValidatableResponse resposta = como(tenta, p.processo(), APROVAR_NA_ALCADA);

        if (esperado == 200) {
            resposta.statusCode(200).body("estado", equalTo(APROVADO));
            assertThat(situacaoDe(p.processo()), equalTo(APROVADO));
            Map<String, Object> entrada = entradasDesde(p.processo(), antes.historico().size()).getFirst();
            assertThat(entrada.get("acao"), equalTo("APROVAR_NA_ALCADA"));
            assertThat(entrada.get("autor"), equalTo(tenta.usuario()));
        } else {
            conferirRecusa(resposta, 403);
            conferirNadaMudouEmAguardandoAlcada(p, antes);
        }
    }

    /** A recusa de R17 (e de papel) deixa tudo como estava: aguardando alçada, as quatro ações, o histórico e a cota. */
    private static void conferirNadaMudouEmAguardandoAlcada(Preparado p, Retrato antes) {
        assertThat(retrato(p), equalTo(antes));
        assertThat(situacaoDe(p.processo()), equalTo(AGUARDANDO_ALCADA));
        assertThat(consultarAcoes(p.processo()), containsInAnyOrder("aprovar-na-alcada", "reprovar",
                "devolver-para-analise", "cancelar"));
        // A cota continua indisponível (R6): o dono não abre outro processo.
        tentarAbrirProcesso(p.cota(), SERVICO).statusCode(409);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { SERVICO, IMOBILIARIO })
    @DisplayName("R17: quem concluiu a análise não aprova (403, continua aguardando alçada); outra pessoa, com os mesmos papéis, aprova")
    void quemConcluiuAAnaliseNaoAprovaMasOutraPessoaDosMesmosPapeisAprova(String tipo) {
        Preparado p = emAlcada(tipo, IdentidadeTeste.CARLA, IdentidadeTeste.CARLA, IdentidadeTeste.ARI);
        Retrato antes = retrato(p);

        conferirRecusa(como(IdentidadeTeste.CARLA, p.processo(), APROVAR_NA_ALCADA), 403);
        conferirNadaMudouEmAguardandoAlcada(p, antes);

        como(IdentidadeTeste.DORA, p.processo(), APROVAR_NA_ALCADA).statusCode(200).body("estado", equalTo(APROVADO));
        assertThat(situacaoDe(p.processo()), equalTo(APROVADO));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { SERVICO, IMOBILIARIO })
    @DisplayName("R17: quem só iniciou a análise, sem concluí-la, pode aprovar")
    void quemSoIniciouAAnalisePodeAprovar(String tipo) {
        Preparado p = emAlcada(tipo, IdentidadeTeste.DORA, IdentidadeTeste.ARI, IdentidadeTeste.ARI);

        como(IdentidadeTeste.DORA, p.processo(), APROVAR_NA_ALCADA).statusCode(200).body("estado", equalTo(APROVADO));
    }

    @Test
    @DisplayName("R17 (imobiliário): quem concluiu só a vistoria pode aprovar; quem concluiu a análise, não")
    void noImobiliarioQuemConcluiuSoAVistoriaPodeAprovar() {
        // Dora concluiu a análise e Carla a vistoria, as duas com os papéis de backoffice e de aprovador.
        Preparado p = emAlcada(IMOBILIARIO, IdentidadeTeste.ANA, IdentidadeTeste.DORA, IdentidadeTeste.CARLA);
        Retrato antes = retrato(p);

        conferirRecusa(como(IdentidadeTeste.DORA, p.processo(), APROVAR_NA_ALCADA), 403);
        conferirNadaMudouEmAguardandoAlcada(p, antes);

        como(IdentidadeTeste.CARLA, p.processo(), APROVAR_NA_ALCADA).statusCode(200).body("estado", equalTo(APROVADO));
        assertThat(entradasDesde(p.processo(), antes.historico().size()).getFirst().get("autor"), equalTo("carla"));
    }

    @Test
    @DisplayName("R17 (imobiliário): quem concluiu a análise e também a vistoria não aprova")
    void noImobiliarioQuemConcluiuAAnaliseEAVistoriaNaoAprova() {
        Preparado p = emAlcada(IMOBILIARIO, IdentidadeTeste.ANA, IdentidadeTeste.DORA, IdentidadeTeste.DORA);
        Retrato antes = retrato(p);

        conferirRecusa(como(IdentidadeTeste.DORA, p.processo(), APROVAR_NA_ALCADA), 403);

        conferirNadaMudouEmAguardandoAlcada(p, antes);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = { SERVICO, IMOBILIARIO })
    @DisplayName("R17: devolvida e concluída por outra pessoa, a regra passa a valer para a pessoa da nova conclusão")
    void depoisDeDevolverVaiValerAPessoaDaUltimaConclusao(String tipo) {
        Preparado p = emAlcada(tipo, IdentidadeTeste.CARLA, IdentidadeTeste.CARLA, IdentidadeTeste.ARI);
        conferirRecusa(como(IdentidadeTeste.CARLA, p.processo(), APROVAR_NA_ALCADA), 403);

        // O aprovador devolve para análise e outra pessoa conclui de novo (no imobiliário, a vistoria se repete).
        como(IdentidadeTeste.BRUNO, p.processo(), DEVOLVER_PARA_ANALISE).statusCode(200)
                .body("estado", equalTo(EM_ANALISE));
        concluirAteAlcada(tipo, p.processo(), IdentidadeTeste.DORA, IdentidadeTeste.ARI);
        Retrato antes = retrato(p);

        // A nova pessoa da conclusão não aprova...
        conferirRecusa(como(IdentidadeTeste.DORA, p.processo(), APROVAR_NA_ALCADA), 403);
        conferirNadaMudouEmAguardandoAlcada(p, antes);
        // ... e quem concluiu na primeira vez, que já não é a última conclusão, aprova.
        como(IdentidadeTeste.CARLA, p.processo(), APROVAR_NA_ALCADA).statusCode(200).body("estado", equalTo(APROVADO));
        assertThat(entradasDesde(p.processo(), antes.historico().size()).getFirst().get("autor"), equalTo("carla"));
    }

    // ---- R19: o autor de cada entrada do histórico ----

    /**
     * Quem faz cada passo nos caminhos do modelo: pessoas diferentes do mesmo papel se alternam (ana e ari no
     * backoffice, bruno e bia na aprovação), para o autor registrado não poder ser uma constante. Quem concluiu a
     * análise nunca é aprovador, para R17 não interferir. Reprovar e devolver são do aprovador em aguardando alçada e do
     * backoffice nas demais; cancelar é do aprovador (também vale para o backoffice, ver a tabela de papéis).
     */
    private static Pessoa atorDoPasso(String acao, String situacaoAnterior, int indice, Pessoa dono) {
        List<Pessoa> backoffice = List.of(IdentidadeTeste.ANA, IdentidadeTeste.ARI);
        List<Pessoa> aprovadores = List.of(IdentidadeTeste.BRUNO, IdentidadeTeste.BIA);
        return switch (acao) {
            case ALTERAR, CONCORDAR, DISCORDAR -> dono;
            case SOLICITAR_PAGAMENTO -> IdentidadeTeste.TEREZA;
            case APROVAR_NA_ALCADA, CANCELAR -> aprovadores.get(indice % 2);
            case REPROVAR, DEVOLVER_PARA_ANALISE -> AGUARDANDO_ALCADA.equals(situacaoAnterior)
                    ? aprovadores.get(indice % 2) : backoffice.get(indice % 2);
            default -> backoffice.get(indice % 2);
        };
    }

    /**
     * Poucos caminhos completos do modelo que, juntos, usam cada ação de pessoa nos dois tipos e cada resultado da
     * rotina (conjunto mínimo guloso, como no modelo de alteração e no de alçada).
     */
    static Stream<Caminho> caminhosDeAutoria() {
        List<Caminho> todos = Stream.of(ModeloPagamentoProcesso.caminhosCompletosPelaAlcada(),
                ModeloPagamentoProcesso.caminhosCompletosComAlteracao(), ModeloPagamentoProcesso.caminhosCompletos())
                .flatMap(Function.identity()).toList();
        Set<String> universo = new LinkedHashSet<>();
        todos.forEach(caminho -> universo.addAll(cobertura(caminho)));
        Set<String> faltando = new HashSet<>(universo);
        List<Caminho> escolhidos = new ArrayList<>();
        while (!faltando.isEmpty()) {
            Caminho melhor = null;
            int ganho = 0;
            for (Caminho caminho : todos) {
                Set<String> novos = new HashSet<>(cobertura(caminho));
                novos.retainAll(faltando);
                if (novos.size() > ganho) {
                    melhor = caminho;
                    ganho = novos.size();
                }
            }
            escolhidos.add(melhor);
            faltando.removeAll(cobertura(melhor));
        }
        return escolhidos.stream();
    }

    private static Set<String> cobertura(Caminho caminho) {
        Set<String> itens = new HashSet<>();
        String anterior = ABERTO;
        for (Passo passo : caminho.passos()) {
            // Reprovar e devolver mudam de papel conforme a situação de origem: contam por situação.
            boolean dependeDaSituacao = REPROVAR.equals(passo.acao()) || DEVOLVER_PARA_ANALISE.equals(passo.acao());
            itens.add(ehDaRotina(passo.acao()) ? passo.acao()
                    : caminho.tipo() + "|" + passo.acao() + (dependeDaSituacao ? "|" + anterior : ""));
            anterior = passo.situacao();
        }
        return itens;
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("caminhosDeAutoria")
    @DisplayName("R19: cada entrada do histórico traz o usuário de quem pediu, e as da rotina, \"sistema\"")
    void autorDeCadaEntradaEQuemPediu(Caminho caminho) {
        long cota = novaCota();
        Pessoa dono = clienteDaCota(cota);
        ProcessoEsperado esperado = ProcessoEsperado.comValor(caminho.valor());
        long processo = abrirProcesso(cota, caminho.tipo(), esperado.valor(), esperado.favorecidos());

        List<String> autoresEsperados = new ArrayList<>();
        List<Passo> passos = caminho.passos();
        String anterior = ABERTO;
        for (int i = 0; i < passos.size(); i++) {
            Passo passo = passos.get(i);
            switch (passo.acao()) {
                case ROTINA_CONFIRMOU -> {
                    TesourariaStub.confirmar(PROTOCOLO_DA_TESOURARIA);
                    aguardarSituacao(processo, PAGO);
                    autoresEsperados.add(SISTEMA);
                }
                case ROTINA_RECUSOU -> {
                    TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
                    aguardarSituacao(processo, APROVADO);
                    TesourariaStub.confirmar();
                    autoresEsperados.add(SISTEMA);
                }
                default -> {
                    if (SOLICITAR_PAGAMENTO.equals(passo.acao())) {
                        String proxima = i + 1 < passos.size() ? passos.get(i + 1).acao() : null;
                        if (ROTINA_CONFIRMOU.equals(proxima)) {
                            TesourariaStub.confirmar();
                        } else if (ROTINA_RECUSOU.equals(proxima)) {
                            TesourariaStub.recusar(MOTIVO_DA_TESOURARIA);
                        } else {
                            TesourariaStub.ficarIndisponivel();
                        }
                    }
                    Pessoa quem = atorDoPasso(passo.acao(), anterior, i, dono);
                    Map<String, Object> corpo = esperado.preparar(passo.acao());
                    executarComo(quem.autorizacao(), processo, passo.acao(), corpo == null ? null : json(corpo))
                            .statusCode(200).body("estado", equalTo(passo.situacao()));
                    esperado.confirmar(passo.acao());
                    autoresEsperados.add(quem.usuario());
                }
            }
            anterior = passo.situacao();
        }

        List<Map<String, Object>> historico = consultarHistoricoSemTentativas(processo);
        assertThat(historico.size(), equalTo(passos.size()));
        for (int i = 0; i < passos.size(); i++) {
            Map<String, Object> item = historico.get(i);
            assertThat("ação da entrada " + i, item.get("acao"), equalTo(nomeNoHistorico(passos.get(i).acao())));
            assertThat("autor da entrada " + i + " (" + item.get("acao") + ")", item.get("autor"),
                    equalTo(autoresEsperados.get(i)));
        }
        // As tentativas da rotina que ficaram sem resultado também são dela.
        consultarHistorico(processo).stream().filter(item -> "TENTAR_PAGAMENTO".equals(item.get("acao")))
                .forEach(item -> assertThat(item.get("autor"), equalTo(SISTEMA)));
    }

    @Test
    @DisplayName("R19: as tentativas da rotina sem resposta e a confirmação têm autor \"sistema\"; a solicitação, a pessoa")
    void tentativasEConfirmacaoDaRotinaTemAutorSistema() {
        TesourariaStub.ficarIndisponivel();
        Preparado p = processoEm(SERVICO, APROVADO, VALOR_PADRAO);
        como(IdentidadeTeste.TEREZA, p.processo(), SOLICITAR_PAGAMENTO).statusCode(200)
                .body("estado", equalTo(PAGAMENTO_SOLICITADO));
        aguardarTentativas(p.processo(), 2);

        List<Map<String, Object>> tentativas = consultarHistorico(p.processo()).stream()
                .filter(item -> "TENTAR_PAGAMENTO".equals(item.get("acao"))).toList();
        assertThat(tentativas.size() >= 2, equalTo(true));
        tentativas.forEach(item -> assertThat(item.get("autor"), equalTo(SISTEMA)));
        List<Map<String, Object>> solicitacoes = consultarHistorico(p.processo()).stream()
                .filter(item -> "SOLICITAR_PAGAMENTO".equals(item.get("acao"))).toList();
        assertThat(solicitacoes.size(), equalTo(1));
        assertThat(solicitacoes.getFirst().get("autor"), equalTo("tereza"));

        TesourariaStub.confirmar();
        aguardarSituacao(p.processo(), PAGO);
        assertThat(consultarHistorico(p.processo()).stream()
                .filter(item -> "CONFIRMAR_PAGAMENTO".equals(item.get("acao"))).map(item -> item.get("autor")).toList(),
                contains(SISTEMA));
    }

    @Test
    @DisplayName("R19: pedidos recusados por papel não entram no histórico, e o histórico segue só com as ações aceitas")
    void recusaPorPapelNaoEntraNoHistorico() {
        long cota = novaCota();
        long processo = abrirProcesso(cota, SERVICO);

        conferirRecusa(como(IdentidadeTeste.TEREZA, processo, INICIAR_ANALISE), 403);
        como(IdentidadeTeste.ANA, processo, INICIAR_ANALISE).statusCode(200);
        conferirRecusa(executarComo(SEM_TOKEN, processo, CONCLUIR_ANALISE, null), 401);
        como(IdentidadeTeste.ARI, processo, CONCLUIR_ANALISE).statusCode(200);

        assertThat(consultarHistorico(processo).stream().map(item -> item.get("acao")).toList(),
                contains("INICIAR_ANALISE", "CONCLUIR_ANALISE"));
        assertThat(autoresDoHistorico(processo), contains("ana", "ari"));
        assertThat(consultarAcoes(processo), hasItem("solicitar-pagamento"));
    }
}
