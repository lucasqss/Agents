package br.com.claudio.pagamento.application.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.claudio.pagamento.application.NaoEncontradoException;
import br.com.claudio.pagamento.domain.AlteracaoConcorrenteException;
import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.pagamento.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import br.com.claudio.pagamento.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Unidade do use case, com o Quarkus montando o bean (a alçada vem do application.properties de verdade) e o repositório
 * e o relógio trocados por mocks: prova a orquestração sem banco. A regra em si é provada no domínio.
 */
@QuarkusTest
class ExecutarAcaoProcessoUseCaseTest {

    private static final Instant AGORA = Instant.parse("2026-03-04T05:06:07Z");

    @Inject
    ExecutarAcaoProcessoUseCase useCase;

    @InjectMock
    ProcessoPagamentoRepository processos;

    @InjectMock
    Clock relogio;

    @BeforeEach
    void configurar() {
        when(relogio.instant()).thenReturn(AGORA);
        when(processos.salvar(any())).thenAnswer(chamada -> chamada.getArgument(0));
    }

    @Test
    void processoInexistenteE404SemGravar() {
        when(processos.buscarPorId(9L)).thenReturn(Optional.empty());
        assertThrows(NaoEncontradoException.class, () -> useCase.executar(9L, AcaoProcesso.INICIAR_ANALISE, "ana", null));
        verify(processos, never()).salvar(any());
    }

    /** A alçada do application.properties (50000.00) chega ao domínio: no limite aprova, um centavo acima aguarda. */
    @Test
    void entregaAAlcadaDaConfiguracao() {
        when(processos.buscarPorId(1L)).thenReturn(Optional.of(emAnalise("50000.00")));
        when(processos.buscarPorId(2L)).thenReturn(Optional.of(emAnalise("50000.01")));

        assertEquals(EstadoProcesso.APROVADO, useCase.executar(1L, AcaoProcesso.CONCLUIR_ANALISE, "ana", null));
        assertEquals(EstadoProcesso.AGUARDANDO_ALCADA, useCase.executar(2L, AcaoProcesso.CONCLUIR_ANALISE, "ana", null));
    }

    @Test
    void gravaATransicaoComAutorMotivoEHoraDoRelogio() {
        when(processos.buscarPorId(1L)).thenReturn(Optional.of(emAnalise("10.00")));

        useCase.executar(1L, AcaoProcesso.REPROVAR, "ana", "fora da política");

        ArgumentCaptor<ProcessoPagamento> gravado = ArgumentCaptor.forClass(ProcessoPagamento.class);
        verify(processos).salvar(gravado.capture());
        assertEquals(List.of(new Transicao(EstadoProcesso.EM_ANALISE, EstadoProcesso.REPROVADO, AcaoProcesso.REPROVAR,
                AGORA, "ana", "fora da política")), gravado.getValue().novasTransicoes());
    }

    @Test
    void recusaDoDominioNaoGrava() {
        when(processos.buscarPorId(1L)).thenReturn(Optional.of(emAnalise("10.00")));
        assertThrows(TransicaoInvalidaException.class, () -> useCase.executar(1L, AcaoProcesso.PAGAR, "ana", null));
        verify(processos, never()).salvar(any());
    }

    @Test
    void conflitoDeVersaoSobeParaQuemChamou() {
        when(processos.buscarPorId(1L)).thenReturn(Optional.of(emAnalise("10.00")));
        when(processos.salvar(any())).thenThrow(new AlteracaoConcorrenteException("Processo", 1L));
        assertThrows(AlteracaoConcorrenteException.class,
                () -> useCase.executar(1L, AcaoProcesso.SOLICITAR_DOCUMENTO, "ana", "falta RG"));
    }

    private static ProcessoPagamento emAnalise(String valor) {
        return ProcessoPagamento.reconstituir(1L, TipoProcesso.SERVICO, "COTA-1", Dinheiro.de(valor),
                EstadoProcesso.EM_ANALISE, 0);
    }
}
