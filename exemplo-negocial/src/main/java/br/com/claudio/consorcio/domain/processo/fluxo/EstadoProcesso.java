package br.com.claudio.consorcio.domain.processo.fluxo;

/**
 * Estados possíveis de um processo, de qualquer tipo. O nome da constante é o código gravado na coluna
 * {@code estado}. Quem diz de qual estado se vai para qual é o {@link FluxoProcesso} de cada tipo.
 * Estado novo: uma constante aqui, as linhas no fluxo que o usa e o rótulo em {@code Rotulos}.
 */
public enum EstadoProcesso {

    ABERTO(false),
    EM_ANALISE(false),
    PENDENTE_DOCUMENTO(false),
    AGUARDANDO_VISTORIA(false),
    AGUARDANDO_CONCORDANCIA(false),
    APROVADO(false),
    PAGO(true),
    REPROVADO(true),
    CANCELADO(true);

    private final boolean finalizado;

    EstadoProcesso(boolean finalizado) {
        this.finalizado = finalizado;
    }

    /** Estado final não aceita nenhuma ação. */
    public boolean finalizado() {
        return finalizado;
    }
}
