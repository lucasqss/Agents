package br.com.claudio.consorcio.domain.processo.fluxo;

/**
 * Ações que movem um processo de um estado para outro. Nem todo tipo usa todas: a ação só vale onde o
 * {@link FluxoProcesso} do tipo tem uma linha para ela.
 * Ação nova: uma constante aqui e as linhas nos fluxos que a aceitam.
 */
public enum AcaoProcesso {

    INICIAR_ANALISE("iniciar a análise de", false),
    SOLICITAR_DOCUMENTO("solicitar documento para", true),
    RECEBER_DOCUMENTO("receber documento de", false),
    REPROVAR("reprovar", true),
    CONCLUIR_ANALISE("concluir a análise de", false),
    CONCLUIR_VISTORIA("concluir a vistoria de", false),
    DEVOLVER_PARA_ANALISE("devolver para análise", true),
    PAGAR("pagar", false),
    CANCELAR("cancelar", true);

    private final String descricao;
    private final boolean exigeMotivo;

    AcaoProcesso(String descricao, boolean exigeMotivo) {
        this.descricao = descricao;
        this.exigeMotivo = exigeMotivo;
    }

    /** Regra de negócio: a ação só vale com o motivo informado (ele fica no histórico, como observação). */
    public boolean exigeMotivo() {
        return exigeMotivo;
    }

    /** Texto da ação para mensagens: "Não é possível [descrição] um processo...". */
    public String descricao() {
        return descricao;
    }
}
