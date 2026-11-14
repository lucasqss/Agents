package br.com.claudio.consorcio.domain.processo;

import static br.com.claudio.consorcio.domain.processo.AutoresDeTeste.*;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.*;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.PermissaoNegadaException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Autoria (R19), papel por linha da tabela (R18) e a regra da segunda pessoa (R17) na raiz do processo. Domínio puro,
 * sem Quarkus. Cada recusa prova também que nada mudou: estado, histórico novo e o usuário da última conclusão.
 */
class AutoriaEPapeisDoProcessoTest {

    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");
    private static final Dinheiro ATE_A_ALCADA = Dinheiro.de("30000.00");
    private static final Dinheiro ACIMA_DA_ALCADA = Dinheiro.de("80000.00");
    private static final Instant AGORA = Instant.parse("2026-01-15T12:00:00Z");

    private static ProcessoPagamento novo(TipoProcesso tipo, Dinheiro valor) {
        return ProcessoPagamento.novo(tipo, 1L, valor, List.of(
                new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", valor)));
    }

    private static void fazer(ProcessoPagamento processo, Autor autor, AcaoProcesso acao) {
        processo.executar(autor, acao, AGORA, motivoSeExige(acao), ALCADA);
    }

    private static String motivoSeExige(AcaoProcesso acao) {
        return acao.exigeMotivo() ? "motivo" : null;
    }

    /** Serviço de valor acima da alçada, levado até Aguardando alçada: quem concluiu a análise é {@code quem}. */
    private static ProcessoPagamento aguardandoAlcada(Autor quemIniciou, Autor quemConcluiu) {
        ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ACIMA_DA_ALCADA);
        fazer(processo, quemIniciou, INICIAR_ANALISE);
        fazer(processo, quemConcluiu, CONCLUIR_ANALISE);
        assertEquals(AGUARDANDO_ALCADA, processo.estado());
        return processo;
    }

    @Nested
    class AutorDoHistorico {

        @Test
        void cadaTransicaoGuardaQuemAPediu() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ACIMA_DA_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ARI, CONCLUIR_ANALISE);
            fazer(processo, BRUNO, APROVAR_NA_ALCADA);
            processo.solicitarPagamento(TEREZA, new IdentificacaoSolicitacao(UUID.randomUUID()), AGORA, null);

            assertEquals(List.of("ana", "ari", "bruno", "tereza"),
                    processo.novasTransicoes().stream().map(Transicao::autor).toList());
        }

        @Test
        void alterarEPorAlteracaoGuardamOAutorEOValor() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            processo.alterar(CLIENTE, Dinheiro.de("100.00"), List.of(
                    new Favorecido("Outro", Documento.de("98765432100"), "vendedor", Dinheiro.de("100.00"))), AGORA);
            fazer(processo, ANA, INICIAR_ANALISE);
            processo.proporAlteracao(ARI, ATE_A_ALCADA, List.of(
                    new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", ATE_A_ALCADA)), "ajuste", AGORA);
            fazer(processo, CLIENTE, CONCORDAR);

            assertEquals(List.of("cliente-1", "ana", "ari", "cliente-1"),
                    processo.novasTransicoes().stream().map(Transicao::autor).toList());
        }

        @Test
        void aRotinaGravaSistema() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            IdentificacaoSolicitacao identificacao = new IdentificacaoSolicitacao(UUID.randomUUID());
            processo.solicitarPagamento(TEREZA, identificacao, AGORA, null);

            processo.aplicarResultado(identificacao, new ResultadoPagamento.SemResposta("sem resposta"), AGORA);
            processo.aplicarResultado(identificacao, new ResultadoPagamento.Recusado("conta inválida"), AGORA);

            List<Transicao> rotina = processo.novasTransicoes().subList(3, 5);
            assertEquals(List.of(Transicao.AUTOR_SISTEMA, Transicao.AUTOR_SISTEMA),
                    rotina.stream().map(Transicao::autor).toList());
        }

        @Test
        void transicaoSemAutorEInvalida() {
            assertThrows(DominioException.class,
                    () -> new Transicao(ABERTO, EM_ANALISE, INICIAR_ANALISE, AGORA, null, null));
            assertThrows(DominioException.class,
                    () -> new Transicao(ABERTO, EM_ANALISE, INICIAR_ANALISE, AGORA, " ", null));
        }

        @Test
        void autorSemUsuarioEInvalido() {
            assertThrows(DominioException.class, () -> new Autor(null, Set.of(Papel.BACKOFFICE), null));
            assertThrows(DominioException.class, () -> new Autor(" ", Set.of(Papel.BACKOFFICE), null));
        }
    }

    @Nested
    class PapelPorLinhaDaTabela {

        @Test
        void quemNaoTemOPapelDaLinhaEhRecusadoENadaMuda() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, BRUNO, INICIAR_ANALISE));
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, TEREZA, INICIAR_ANALISE));
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, CLIENTE, INICIAR_ANALISE));
            assertEquals(ABERTO, processo.estado());
            assertTrue(processo.novasTransicoes().isEmpty());
        }

        @Test
        void usuarioSemPapelNenhumEhRecusado() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            Autor visitante = new Autor("visitante", Set.of(), null);

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, visitante, INICIAR_ANALISE));
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, visitante, CANCELAR));
        }

        @Test
        void reprovarEmAnaliseEDoBackofficeEOAprovadorNaoPode() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, BRUNO, REPROVAR));
            assertEquals(EM_ANALISE, processo.estado());
            fazer(processo, ARI, REPROVAR);
            assertEquals(REPROVADO, processo.estado());
        }

        @Test
        void reprovarEmAguardandoAlcadaEDoAprovadorEOBackofficeNaoPode() {
            ProcessoPagamento recusado = aguardandoAlcada(ANA, ANA);
            assertThrows(PermissaoNegadaException.class, () -> fazer(recusado, ARI, REPROVAR));
            assertEquals(AGUARDANDO_ALCADA, recusado.estado());

            ProcessoPagamento aceito = aguardandoAlcada(ANA, ANA);
            fazer(aceito, BRUNO, REPROVAR);
            assertEquals(REPROVADO, aceito.estado());
        }

        @Test
        void devolverDeAprovadoEDoBackofficeDeAguardandoAlcadaEDoAprovador() {
            ProcessoPagamento aprovado = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(aprovado, ANA, INICIAR_ANALISE);
            fazer(aprovado, ANA, CONCLUIR_ANALISE);
            assertThrows(PermissaoNegadaException.class, () -> fazer(aprovado, BRUNO, DEVOLVER_PARA_ANALISE));
            fazer(aprovado, ARI, DEVOLVER_PARA_ANALISE);
            assertEquals(EM_ANALISE, aprovado.estado());

            ProcessoPagamento naAlcada = aguardandoAlcada(ANA, ANA);
            assertThrows(PermissaoNegadaException.class, () -> fazer(naAlcada, ARI, DEVOLVER_PARA_ANALISE));
            fazer(naAlcada, BRUNO, DEVOLVER_PARA_ANALISE);
            assertEquals(EM_ANALISE, naAlcada.estado());
        }

        @Test
        void devolverDeAguardandoVistoriaEDoBackoffice() {
            ProcessoPagamento processo = novo(TipoProcesso.IMOBILIARIO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            assertEquals(AGUARDANDO_VISTORIA, processo.estado());

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, BRUNO, DEVOLVER_PARA_ANALISE));
            fazer(processo, ARI, DEVOLVER_PARA_ANALISE);
            assertEquals(EM_ANALISE, processo.estado());
        }

        @Test
        void umaPessoaComOsDoisPapeisExerceOsDois() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ACIMA_DA_ALCADA);
            fazer(processo, CARLA, INICIAR_ANALISE);
            fazer(processo, CARLA, CONCLUIR_ANALISE);
            fazer(processo, CARLA, DEVOLVER_PARA_ANALISE);
            assertEquals(EM_ANALISE, processo.estado());
        }

        @Test
        void cancelarEDoBackofficeEDoAprovadorNaoDoClienteNemDaTesouraria() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, CLIENTE, CANCELAR));
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, TEREZA, CANCELAR));
            fazer(processo, BRUNO, CANCELAR);
            assertEquals(CANCELADO, processo.estado());
        }

        @Test
        void soATesourariaSolicitaOPagamento() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            IdentificacaoSolicitacao identificacao = new IdentificacaoSolicitacao(UUID.randomUUID());

            assertThrows(PermissaoNegadaException.class,
                    () -> processo.solicitarPagamento(ANA, identificacao, AGORA, null));
            assertEquals(APROVADO, processo.estado());
            processo.solicitarPagamento(TEREZA, identificacao, AGORA, null);
            assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        }

        @Test
        void soOClienteAlteraEmAbertoEOBackofficeNaoPode() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            List<Favorecido> novos = List.of(
                    new Favorecido("Outro", Documento.de("98765432100"), "vendedor", Dinheiro.de("100.00")));

            assertThrows(PermissaoNegadaException.class, () -> processo.alterar(ANA, Dinheiro.de("100.00"), novos, AGORA));
            assertEquals(ATE_A_ALCADA, processo.valor());
            processo.alterar(CLIENTE, Dinheiro.de("100.00"), novos, AGORA);
            assertEquals(Dinheiro.de("100.00"), processo.valor());
        }

        @Test
        void soOBackofficeProporAlteracaoEOClienteNaoPode() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            List<Favorecido> novos = List.of(
                    new Favorecido("Outro", Documento.de("98765432100"), "vendedor", Dinheiro.de("100.00")));

            assertThrows(PermissaoNegadaException.class,
                    () -> processo.proporAlteracao(CLIENTE, Dinheiro.de("100.00"), novos, "m", AGORA));
            assertTrue(processo.propostaPendente().isEmpty());
            processo.proporAlteracao(ANA, Dinheiro.de("100.00"), novos, "m", AGORA);
            assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
        }

        @Test
        void soOClienteConcordaEDiscordaOBackofficeNaoPode() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            processo.proporAlteracao(ANA, Dinheiro.de("100.00"), List.of(
                    new Favorecido("Outro", Documento.de("98765432100"), "vendedor", Dinheiro.de("100.00"))), "m", AGORA);

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, ANA, CONCORDAR));
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, CARLA, DISCORDAR));
            assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
            fazer(processo, CLIENTE, CONCORDAR);
            assertEquals(EM_ANALISE, processo.estado());
        }

        @Test
        void aLinhaQueNaoValeNaSituacaoEhRecusadaPelaTransicaoEPorMaisQueOAutorTenhaOPapel() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);

            assertThrows(TransicaoInvalidaException.class, () -> fazer(processo, ANA, APROVAR_NA_ALCADA));
            assertThrows(TransicaoInvalidaException.class, () -> fazer(processo, BRUNO, APROVAR_NA_ALCADA));
        }

        @ParameterizedTest
        @EnumSource(value = AcaoProcesso.class, names = { "CONFIRMAR_PAGAMENTO", "RECUSAR_PAGAMENTO", "TENTAR_PAGAMENTO" })
        void pessoaNenhumaPedeAAcaoDaRotina(AcaoProcesso acao) {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            processo.solicitarPagamento(TEREZA, new IdentificacaoSolicitacao(UUID.randomUUID()), AGORA, null);
            Autor todos = new Autor("todos", Set.of(Papel.values()), 1L);

            assertThrows(DominioException.class, () -> processo.executar(todos, acao, AGORA, "x", ALCADA));
            assertEquals(PAGAMENTO_SOLICITADO, processo.estado());
        }

        @Test
        void autorNuloEhRecusado() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            assertThrows(DominioException.class, () -> processo.executar(null, INICIAR_ANALISE, AGORA, null, ALCADA));
        }

        @Test
        void quemExigeOClienteDaCotaEstaNaTabela() {
            ProcessoPagamento processo = novo(TipoProcesso.SERVICO, ATE_A_ALCADA);
            assertTrue(processo.exigeClienteDaCota(ALTERAR));
            assertFalse(processo.exigeClienteDaCota(INICIAR_ANALISE));
            assertFalse(processo.exigeClienteDaCota(CONCORDAR), "não vale em Aberto");
            fazer(processo, ANA, INICIAR_ANALISE);
            processo.proporAlteracao(ANA, Dinheiro.de("100.00"), List.of(
                    new Favorecido("Outro", Documento.de("98765432100"), "vendedor", Dinheiro.de("100.00"))), "m", AGORA);
            assertTrue(processo.exigeClienteDaCota(CONCORDAR));
            assertTrue(processo.exigeClienteDaCota(DISCORDAR));
            assertFalse(processo.exigeClienteDaCota(CANCELAR));
        }
    }

    @Nested
    class SegundaPessoaNaAlcada {

        @Test
        void quemConcluiuAAnaliseNaoAprova() {
            ProcessoPagamento processo = aguardandoAlcada(ANA, CARLA);

            PermissaoNegadaException recusa = assertThrows(PermissaoNegadaException.class,
                    () -> fazer(processo, CARLA, APROVAR_NA_ALCADA));
            assertTrue(recusa.getMessage().contains("R17"), recusa.getMessage());
            assertEquals(AGUARDANDO_ALCADA, processo.estado());
            assertEquals(2, processo.novasTransicoes().size());
        }

        @Test
        void outraPessoaDoMesmoPapelAprova() {
            ProcessoPagamento processo = aguardandoAlcada(ANA, CARLA);
            Autor dora = new Autor("dora", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);

            fazer(processo, dora, APROVAR_NA_ALCADA);
            assertEquals(APROVADO, processo.estado());
        }

        @Test
        void quemSoIniciouAAnalisePodeAprovar() {
            Autor iniciou = new Autor("ivo", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);
            ProcessoPagamento processo = aguardandoAlcada(iniciou, ANA);

            fazer(processo, iniciou, APROVAR_NA_ALCADA);
            assertEquals(APROVADO, processo.estado());
        }

        @Test
        void aRegraEPorUsuarioNaoPorPapel() {
            ProcessoPagamento processo = aguardandoAlcada(ANA, ANA);
            Autor anaComoAprovador = new Autor("ana", Set.of(Papel.APROVADOR), null);
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, anaComoAprovador, APROVAR_NA_ALCADA));
            fazer(processo, BRUNO, APROVAR_NA_ALCADA);
        }

        @Test
        void depoisDeDevolverVaiValerAPessoaDaNovaConclusao() {
            Autor ivo = new Autor("ivo", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);
            ProcessoPagamento processo = aguardandoAlcada(ANA, ivo);
            assertEquals("ivo", processo.autorDaUltimaConclusaoDaAnalise().orElseThrow());

            fazer(processo, BRUNO, DEVOLVER_PARA_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            assertEquals("ana", processo.autorDaUltimaConclusaoDaAnalise().orElseThrow());

            // Quem concluiu antes (ivo) agora pode aprovar; quem concluiu por último (ana) não.
            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, new Autor("ana", Set.of(Papel.APROVADOR), null), APROVAR_NA_ALCADA));
            fazer(processo, ivo, APROVAR_NA_ALCADA);
            assertEquals(APROVADO, processo.estado());
        }

        @Test
        void noImobiliarioQuemConcluiuSoAVistoriaPodeAprovar() {
            Autor vera = new Autor("vera", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);
            ProcessoPagamento processo = novo(TipoProcesso.IMOBILIARIO, ACIMA_DA_ALCADA);
            fazer(processo, ANA, INICIAR_ANALISE);
            fazer(processo, ANA, CONCLUIR_ANALISE);
            fazer(processo, vera, CONCLUIR_VISTORIA);
            assertEquals(AGUARDANDO_ALCADA, processo.estado());

            fazer(processo, vera, APROVAR_NA_ALCADA);
            assertEquals(APROVADO, processo.estado());
        }

        @Test
        void noImobiliarioQuemConcluiuAAnaliseEAVistoriaNaoAprova() {
            Autor vera = new Autor("vera", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);
            ProcessoPagamento processo = novo(TipoProcesso.IMOBILIARIO, ACIMA_DA_ALCADA);
            fazer(processo, vera, INICIAR_ANALISE);
            fazer(processo, vera, CONCLUIR_ANALISE);
            fazer(processo, vera, CONCLUIR_VISTORIA);

            assertThrows(PermissaoNegadaException.class, () -> fazer(processo, vera, APROVAR_NA_ALCADA));
            assertEquals(AGUARDANDO_ALCADA, processo.estado());
        }

        @Test
        void semConclusaoRegistradaARegraNaoImpedeNinguem() {
            // Processo gravado antes da fatia 7: o histórico antigo não diz quem concluiu a análise.
            ProcessoPagamento antigo = ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, ACIMA_DA_ALCADA,
                    List.of(new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", ACIMA_DA_ALCADA)),
                    AGUARDANDO_ALCADA, null, null, null, null, 3);

            assertTrue(antigo.autorDaUltimaConclusaoDaAnalise().isEmpty());
            fazer(antigo, new Autor("ana", Set.of(Papel.APROVADOR), null), APROVAR_NA_ALCADA);
            assertEquals(APROVADO, antigo.estado());
        }

        @Test
        void oUsuarioDaConclusaoVoltaDoBancoEFicaNoAgregado() {
            ProcessoPagamento lido = ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, ACIMA_DA_ALCADA,
                    List.of(new Favorecido("Favorecido", Documento.de("12345678901"), "vendedor", ACIMA_DA_ALCADA)),
                    AGUARDANDO_ALCADA, null, null, null, "ana", 3);

            assertThrows(PermissaoNegadaException.class,
                    () -> fazer(lido, new Autor("ana", Set.of(Papel.APROVADOR), null), APROVAR_NA_ALCADA));
            fazer(lido, BRUNO, APROVAR_NA_ALCADA);
        }
    }
}
