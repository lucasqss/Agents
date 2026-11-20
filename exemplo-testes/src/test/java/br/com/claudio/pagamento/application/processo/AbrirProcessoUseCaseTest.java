package br.com.claudio.pagamento.application.processo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.claudio.pagamento.domain.DominioException;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

@QuarkusTest
class AbrirProcessoUseCaseTest {

    @Inject
    AbrirProcessoUseCase useCase;

    @InjectMock
    ProcessoPagamentoRepository processos;

    @Test
    void gravaOProcessoAbertoEDevolveOIdGerado() {
        when(processos.salvar(any())).thenAnswer(chamada -> {
            ProcessoPagamento novo = chamada.getArgument(0);
            return ProcessoPagamento.reconstituir(42L, novo.tipo(), novo.numeroCota(), novo.valor(), novo.estado(), 0);
        });

        assertEquals(42L, useCase.executar(TipoProcesso.IMOBILIARIO, "COTA-9", new BigDecimal("100.50")));

        ArgumentCaptor<ProcessoPagamento> gravado = ArgumentCaptor.forClass(ProcessoPagamento.class);
        verify(processos).salvar(gravado.capture());
        assertEquals(EstadoProcesso.ABERTO, gravado.getValue().estado());
        assertEquals(TipoProcesso.IMOBILIARIO, gravado.getValue().tipo());
        assertEquals(new BigDecimal("100.50"), gravado.getValue().valor().valor());
    }

    @Test
    void valorInvalidoNaoChegaAoRepositorio() {
        assertThrows(DominioException.class, () -> useCase.executar(TipoProcesso.SERVICO, "COTA-9", BigDecimal.ZERO));
        verify(processos, never()).salvar(any());
    }
}
