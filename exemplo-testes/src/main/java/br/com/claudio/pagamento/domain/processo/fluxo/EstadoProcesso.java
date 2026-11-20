package br.com.claudio.pagamento.domain.processo.fluxo;

/**
 * Estados possíveis de um processo, de qualquer tipo. O nome da constante é o código gravado na coluna {@code estado}.
 * Quem diz de qual estado se vai para qual é o {@link FluxoProcesso} de cada tipo.
 * Estado novo: uma constante aqui e as linhas no fluxo que o usa.
 */
public enum EstadoProcesso {

    ABERTO(false),
    EM_ANALISE(false),
    PENDENTE_DOCUMENTO(false),
    AGUARDANDO_VISTORIA(false),
    AGUARDANDO_ALCADA(false),
    APROVADO(false),
    PAGO(true),
    REPROVADO(true),
    CANCELADO(true);

    private final boolean finalizado;

    EstadoProcesso(boolean finalizado) {
        this.finalizado = finalizado;
    }

    /** R6: estado final não aceita nenhuma ação. */
    public boolean finalizado() {
        return finalizado;
    }
}
