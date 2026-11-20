package br.com.claudio.pagamento.domain.processo.fluxo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import br.com.claudio.pagamento.domain.DominioException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Os limites das colunas do histórico (autor e observação) são regra do domínio, conferida no limite exato. */
class TransicaoTest {

    private static final Instant AGORA = Instant.parse("2026-01-02T10:00:00Z");

    @Test
    void autorAteOLimite() {
        assertEquals(100, transicao("a".repeat(100), null).autor().length());
        assertThrows(DominioException.class, () -> transicao("a".repeat(101), null));
    }

    @Test
    void observacaoAteOLimite() {
        assertEquals(500, transicao("ana", "o".repeat(500)).observacao().length());
        assertThrows(DominioException.class, () -> transicao("ana", "o".repeat(501)));
    }

    @Test
    void observacaoEmBrancoViraAusente() {
        assertNull(transicao("ana", "   ").observacao());
    }

    @Test
    void dataObrigatoria() {
        assertThrows(DominioException.class, () -> new Transicao(EstadoProcesso.ABERTO, EstadoProcesso.EM_ANALISE,
                AcaoProcesso.INICIAR_ANALISE, null, "ana", null));
    }

    private static Transicao transicao(String autor, String observacao) {
        return new Transicao(EstadoProcesso.ABERTO, EstadoProcesso.EM_ANALISE, AcaoProcesso.INICIAR_ANALISE, AGORA, autor,
                observacao);
    }
}
