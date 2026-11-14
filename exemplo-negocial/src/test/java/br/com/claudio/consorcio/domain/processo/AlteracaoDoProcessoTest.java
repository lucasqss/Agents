package br.com.claudio.consorcio.domain.processo;

import static br.com.claudio.consorcio.domain.processo.AutoresDeTeste.*;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.ALTERAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CANCELAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.CONCORDAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.DISCORDAR;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.INICIAR_ANALISE;
import static br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso.PROPOR_ALTERACAO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.ABERTO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.AGUARDANDO_CONCORDANCIA;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.CANCELADO;
import static br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso.EM_ANALISE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Alterar (R24), propor alteração (R25), concordar (R26), discordar (R27) e cancelar com proposta (R28) na raiz. */
class AlteracaoDoProcessoTest {

    private static final Dinheiro VALOR = Dinheiro.de("30000.00");
    private static final Dinheiro NOVO_VALOR = Dinheiro.de("25000.00");
    private static final Instant AGORA = Instant.parse("2026-01-15T12:00:00Z");
    private static final Dinheiro ALCADA = Dinheiro.de("50000.00");

    private static List<Favorecido> favorecidos(String documento, Dinheiro parte) {
        return List.of(new Favorecido("Nome " + documento, Documento.de(documento), "vendedor", parte));
    }

    private static final List<Favorecido> VIGENTES = favorecidos("12345678901", VALOR);
    private static final List<Favorecido> NOVOS = favorecidos("98765432100", NOVO_VALOR);

    private static ProcessoPagamento aberto(TipoProcesso tipo) {
        return ProcessoPagamento.novo(tipo, 1L, VALOR, VIGENTES);
    }

    private static ProcessoPagamento emAnalise() {
        ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);
        processo.executar(quemFaz(INICIAR_ANALISE), INICIAR_ANALISE, AGORA, null, ALCADA);
        return processo;
    }

    private static ProcessoPagamento aguardando() {
        ProcessoPagamento processo = emAnalise();
        processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "valor errado", AGORA);
        return processo;
    }

    private static Transicao ultima(ProcessoPagamento processo) {
        return processo.novasTransicoes().getLast();
    }

    @Nested
    class AlterarEmAberto {

        @ParameterizedTest
        @EnumSource(TipoProcesso.class)
        void trocaValorEFavorecidosEContinuaAberto(TipoProcesso tipo) {
            ProcessoPagamento processo = aberto(tipo);

            processo.alterar(CLIENTE, NOVO_VALOR, NOVOS, AGORA);

            assertEquals(ABERTO, processo.estado());
            assertEquals(NOVO_VALOR, processo.valor());
            assertEquals(NOVOS, processo.favorecidos());
            assertTrue(processo.propostaPendente().isEmpty());
        }

        @Test
        void historicoTrazOValorAnteriorEONovo() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            processo.alterar(CLIENTE, NOVO_VALOR, NOVOS, AGORA);

            Transicao transicao = ultima(processo);
            assertEquals(ALTERAR, transicao.acao());
            assertEquals(ABERTO, transicao.de());
            assertEquals(ABERTO, transicao.para());
            assertEquals(VALOR, transicao.valorAnterior());
            assertEquals(NOVO_VALOR, transicao.valorNovo());
        }

        @Test
        void podeAlterarVariasVezes() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            processo.alterar(CLIENTE, NOVO_VALOR, NOVOS, AGORA);
            processo.alterar(CLIENTE, VALOR, VIGENTES, AGORA);

            assertEquals(VALOR, processo.valor());
            assertEquals(VIGENTES, processo.favorecidos());
            assertEquals(2, processo.novasTransicoes().size());
            assertEquals(NOVO_VALOR, ultima(processo).valorAnterior());
        }

        @Test
        void mesmoValorEFavorecidosSaoAceitos() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            processo.alterar(CLIENTE, VALOR, VIGENTES, AGORA);

            assertEquals(1, processo.novasTransicoes().size());
        }

        @Test
        void usaAsMesmasInvariantesDaAbertura() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, null, NOVOS, AGORA));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, Dinheiro.ZERO, NOVOS, AGORA));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, null, AGORA));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, List.of(), AGORA));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, favorecidos("98765432100", Dinheiro.de("24999.99")), AGORA));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, favorecidos("98765432100", Dinheiro.de("25000.01")), AGORA));
            Favorecido a = new Favorecido("A", Documento.de("98765432100"), "vendedor", Dinheiro.de("12500.00"));
            Favorecido b = new Favorecido("B", Documento.de("98765432100"), "cartório", Dinheiro.de("12500.00"));
            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, List.of(a, b), AGORA));
        }

        @Test
        void recusaNaoMudaNada() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            assertThrows(DominioException.class,
                    () -> processo.alterar(CLIENTE, NOVO_VALOR, favorecidos("98765432100", Dinheiro.de("1.00")), AGORA));

            assertEquals(VALOR, processo.valor());
            assertEquals(VIGENTES, processo.favorecidos());
            assertTrue(processo.novasTransicoes().isEmpty());
        }

        @Test
        void soValeEmAberto() {
            ProcessoPagamento processo = emAnalise();

            assertThrows(TransicaoInvalidaException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, NOVOS, AGORA));

            assertEquals(VALOR, processo.valor());
            assertEquals(EM_ANALISE, processo.estado());
        }

        @Test
        void dadoInvalidoVemAntesDoEstado() {
            ProcessoPagamento processo = emAnalise();

            assertThrows(DominioException.class, () -> processo.alterar(CLIENTE, NOVO_VALOR, List.of(), AGORA));
        }
    }

    @Nested
    class ProporAlteracao {

        @ParameterizedTest
        @EnumSource(TipoProcesso.class)
        void guardaAPropostaSemTocarNosVigentes(TipoProcesso tipo) {
            ProcessoPagamento processo = aberto(tipo);
            processo.executar(quemFaz(INICIAR_ANALISE), INICIAR_ANALISE, AGORA, null, ALCADA);

            processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "valor errado", AGORA);

            assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
            assertEquals(VALOR, processo.valor());
            assertEquals(VIGENTES, processo.favorecidos());
            AlteracaoProposta proposta = processo.propostaPendente().orElseThrow();
            assertEquals(NOVO_VALOR, proposta.valor());
            assertEquals(NOVOS, proposta.favorecidos());
            assertEquals("valor errado", proposta.motivo());
            assertTrue(processo.ativo());
        }

        @Test
        void historicoTrazValoresEOMotivoComoObservacao() {
            ProcessoPagamento processo = aguardando();

            Transicao transicao = ultima(processo);
            assertEquals(PROPOR_ALTERACAO, transicao.acao());
            assertEquals(EM_ANALISE, transicao.de());
            assertEquals(AGUARDANDO_CONCORDANCIA, transicao.para());
            assertEquals("valor errado", transicao.observacao());
            assertEquals(VALOR, transicao.valorAnterior());
            assertEquals(NOVO_VALOR, transicao.valorNovo());
        }

        @Test
        void motivoEObrigatorio() {
            ProcessoPagamento processo = emAnalise();

            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, null, AGORA));
            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "   ", AGORA));
            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "x".repeat(501), AGORA));

            assertEquals(EM_ANALISE, processo.estado());
            assertTrue(processo.propostaPendente().isEmpty());
        }

        @Test
        void propostaTemAsMesmasInvariantesDoConjunto() {
            ProcessoPagamento processo = emAnalise();

            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, Dinheiro.ZERO, NOVOS, "m", AGORA));
            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, List.of(), "m", AGORA));
            assertThrows(DominioException.class,
                    () -> processo.proporAlteracao(ANA, NOVO_VALOR, favorecidos("98765432100", Dinheiro.de("24999.99")), "m", AGORA));
            Favorecido a = new Favorecido("A", Documento.de("98765432100"), "vendedor", Dinheiro.de("12500.00"));
            assertThrows(DominioException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, List.of(a, a), "m", AGORA));

            assertEquals(EM_ANALISE, processo.estado());
            assertEquals(1, processo.novasTransicoes().size(), "só a transição de iniciar a análise");
        }

        @Test
        void propostaPodeRepetirODocumentoDeUmVigente() {
            ProcessoPagamento processo = emAnalise();

            processo.proporAlteracao(ANA, NOVO_VALOR, favorecidos("12345678901", NOVO_VALOR), "mesmo favorecido", AGORA);

            assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
        }

        @Test
        void soValeEmAnalise() {
            ProcessoPagamento processo = aberto(TipoProcesso.SERVICO);

            assertThrows(TransicaoInvalidaException.class, () -> processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "m", AGORA));
            assertTrue(processo.propostaPendente().isEmpty());
        }

        @Test
        void naoPodeProporDuasVezesSeguidas() {
            ProcessoPagamento processo = aguardando();

            assertThrows(TransicaoInvalidaException.class, () -> processo.proporAlteracao(ANA, VALOR, VIGENTES, "outra", AGORA));
            assertEquals(NOVO_VALOR, processo.propostaPendente().orElseThrow().valor());
        }
    }

    @Nested
    class RespostaDoCliente {

        @Test
        void concordarTornaAPropostaVigenteEElaSome() {
            ProcessoPagamento processo = aguardando();

            processo.executar(quemFaz(CONCORDAR), CONCORDAR, AGORA, null, ALCADA);

            assertEquals(EM_ANALISE, processo.estado());
            assertEquals(NOVO_VALOR, processo.valor());
            assertEquals(NOVOS, processo.favorecidos());
            assertTrue(processo.propostaPendente().isEmpty());
            Transicao transicao = ultima(processo);
            assertEquals(CONCORDAR, transicao.acao());
            assertNull(transicao.valorAnterior());
            assertNull(transicao.valorNovo());
        }

        @Test
        void concordarGuardaAObservacaoOpcional() {
            ProcessoPagamento processo = aguardando();

            processo.executar(quemFaz(CONCORDAR), CONCORDAR, AGORA, "de acordo", ALCADA);

            assertEquals("de acordo", ultima(processo).observacao());
        }

        @Test
        void discordarDescartaAPropostaEMantemOsVigentes() {
            ProcessoPagamento processo = aguardando();

            processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, "valor errado", ALCADA);

            assertEquals(EM_ANALISE, processo.estado());
            assertEquals(VALOR, processo.valor());
            assertEquals(VIGENTES, processo.favorecidos());
            assertTrue(processo.propostaPendente().isEmpty());
            Transicao transicao = ultima(processo);
            assertEquals(DISCORDAR, transicao.acao());
            assertEquals("valor errado", transicao.observacao());
            assertNull(transicao.valorNovo());
        }

        @Test
        void discordarExigeMotivoENadaMuda() {
            ProcessoPagamento processo = aguardando();

            assertThrows(DominioException.class, () -> processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, null, ALCADA));
            assertThrows(DominioException.class, () -> processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, "  ", ALCADA));

            assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
            assertTrue(processo.propostaPendente().isPresent());
        }

        @Test
        void cancelarDescartaAPropostaEMantemOsVigentes() {
            ProcessoPagamento processo = aguardando();

            processo.executar(quemFaz(CANCELAR), CANCELAR, AGORA, "desistência", ALCADA);

            assertEquals(CANCELADO, processo.estado());
            assertEquals(VALOR, processo.valor());
            assertEquals(VIGENTES, processo.favorecidos());
            assertTrue(processo.propostaPendente().isEmpty());
        }

        @Test
        void podeProporDeNovoDepoisDeDiscordarOuConcordar() {
            ProcessoPagamento processo = aguardando();
            processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, "não", ALCADA);
            processo.proporAlteracao(ANA, Dinheiro.de("20000.00"), favorecidos("11111111111", Dinheiro.de("20000.00")), "outra", AGORA);
            processo.executar(quemFaz(CONCORDAR), CONCORDAR, AGORA, null, ALCADA);

            assertEquals(Dinheiro.de("20000.00"), processo.valor());
            assertEquals(5, processo.novasTransicoes().size(), "iniciar, propor, discordar, propor, concordar");
        }

        @Test
        void respostaForaDeAguardandoERecusada() {
            ProcessoPagamento processo = emAnalise();

            assertThrows(TransicaoInvalidaException.class, () -> processo.executar(quemFaz(CONCORDAR), CONCORDAR, AGORA, null, ALCADA));
            assertThrows(TransicaoInvalidaException.class, () -> processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, "m", ALCADA));
        }
    }

    @Nested
    class EmAguardandoConcordancia {

        @ParameterizedTest
        @EnumSource(AcaoProcesso.class)
        void soConcordarDiscordarECancelarValem(AcaoProcesso acao) {
            ProcessoPagamento processo = aguardando();
            boolean vale = Set.of(CONCORDAR, DISCORDAR, CANCELAR).contains(acao);

            if (vale) {
                assertTrue(processo.acoesDisponiveis().contains(acao));
            } else {
                assertThrows(TransicaoInvalidaException.class, () -> processo.executar(quemFaz(acao), acao, AGORA, "motivo", ALCADA));
                assertEquals(AGUARDANDO_CONCORDANCIA, processo.estado());
                assertEquals(NOVO_VALOR, processo.propostaPendente().orElseThrow().valor());
            }
        }

        @Test
        void acoesDisponiveisSaoExatamenteTres() {
            assertEquals(Set.of(CONCORDAR, DISCORDAR, CANCELAR), aguardando().acoesDisponiveis());
        }

        @Test
        void aguardandoConcordanciaNaoEFinal() {
            assertTrue(aguardando().ativo());
            assertFalse(AGUARDANDO_CONCORDANCIA.finalizado());
        }
    }

    @Nested
    class AcaoComDadoProprio {

        @Test
        void executarGenericoRecusaAlterarEProporAlteracao() {
            ProcessoPagamento aberto = aberto(TipoProcesso.SERVICO);
            assertThrows(DominioException.class, () -> aberto.executar(quemFaz(ALTERAR), ALTERAR, AGORA, null, ALCADA));
            ProcessoPagamento emAnalise = emAnalise();
            assertThrows(DominioException.class, () -> emAnalise.executar(quemFaz(PROPOR_ALTERACAO), PROPOR_ALTERACAO, AGORA, "motivo", ALCADA));

            assertEquals(ABERTO, aberto.estado());
            assertEquals(EM_ANALISE, emAnalise.estado());
            assertTrue(emAnalise.propostaPendente().isEmpty());
        }
    }

    @Nested
    class InvarianteDaProposta {

        private final AlteracaoProposta proposta = new AlteracaoProposta(NOVO_VALOR, NOVOS, "motivo");

        private ProcessoPagamento reconstituir(EstadoProcesso estado, AlteracaoProposta comProposta) {
            return ProcessoPagamento.reconstituir(1L, 1L, TipoProcesso.SERVICO, VALOR, VIGENTES, estado, null, comProposta, null, null, 2);
        }

        @Test
        void reconstituiAguardandoComProposta() {
            ProcessoPagamento processo = reconstituir(AGUARDANDO_CONCORDANCIA, proposta);

            assertEquals(proposta, processo.propostaPendente().orElseThrow());
            assertEquals(VALOR, processo.valor());
            assertTrue(processo.novasTransicoes().isEmpty());
        }

        @Test
        void aguardandoSemPropostaEInconsistente() {
            assertThrows(DominioException.class, () -> reconstituir(AGUARDANDO_CONCORDANCIA, null));
        }

        @ParameterizedTest
        @EnumSource(value = EstadoProcesso.class, names = {"ABERTO", "EM_ANALISE", "APROVADO", "CANCELADO"})
        void propostaForaDeAguardandoEInconsistente(EstadoProcesso estado) {
            assertThrows(DominioException.class, () -> reconstituir(estado, proposta));
        }

        @Test
        void propostaPendenteSempreSegueOEstado() {
            ProcessoPagamento processo = emAnalise();
            assertFalse(processo.propostaPendente().isPresent());
            processo.proporAlteracao(ANA, NOVO_VALOR, NOVOS, "m", AGORA);
            assertTrue(processo.propostaPendente().isPresent());
            processo.executar(quemFaz(DISCORDAR), DISCORDAR, AGORA, "m", ALCADA);
            assertFalse(processo.propostaPendente().isPresent());
        }

        @Test
        void propostaValidaSeusDados() {
            assertThrows(DominioException.class, () -> new AlteracaoProposta(NOVO_VALOR, NOVOS, " "));
            assertThrows(DominioException.class, () -> new AlteracaoProposta(NOVO_VALOR, favorecidos("98765432100", VALOR), "m"));
            assertEquals("m", new AlteracaoProposta(NOVO_VALOR, NOVOS, "  m  ").motivo());
        }
    }

    @Nested
    class HistoricoComValores {

        @Test
        void valoresSoVemJuntosEMotivoNaoSeMistura() {
            assertThrows(DominioException.class,
                    () -> new Transicao(ABERTO, ABERTO, ALTERAR, AGORA, usuarioDe(ALTERAR), null, VALOR, null));
            assertThrows(DominioException.class,
                    () -> new Transicao(ABERTO, ABERTO, ALTERAR, AGORA, usuarioDe(ALTERAR), null, null, VALOR));
            Transicao semValores = new Transicao(ABERTO, EM_ANALISE, INICIAR_ANALISE, AGORA, usuarioDe(INICIAR_ANALISE), null);
            assertNull(semValores.valorAnterior());
            assertNull(semValores.valorNovo());
        }
    }
}
