package br.com.claudio.consorcio.domain.processo.fluxo;

/**
 * Ações que movem um processo de um estado para outro. Nem todo tipo usa todas: a ação só vale onde o
 * {@link FluxoProcesso} do tipo tem uma linha para ela.
 * Ação nova: uma constante aqui e as linhas nos fluxos que a aceitam.
 */
public enum AcaoProcesso {

    INICIAR_ANALISE("iniciar a análise de"),
    CONCLUIR_ANALISE("concluir a análise de"),
    CONCLUIR_VISTORIA("concluir a vistoria de"),
    DEVOLVER_PARA_ANALISE("devolver para análise"),
    PAGAR("pagar"),
    CANCELAR("cancelar");

    private final String descricao;

    AcaoProcesso(String descricao) {
        this.descricao = descricao;
    }

    /** Texto da ação para mensagens: "Não é possível [descrição] um processo...". */
    public String descricao() {
        return descricao;
    }
}
