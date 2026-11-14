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
    SOLICITAR_PAGAMENTO("solicitar o pagamento de", false, true, false),
    CONFIRMAR_PAGAMENTO("confirmar o pagamento de", false, true, true),
    RECUSAR_PAGAMENTO("recusar o pagamento de", true, true, true),
    TENTAR_PAGAMENTO("tentar o pagamento de", true, true, true),
    CANCELAR("cancelar", true),
    ALTERAR("alterar", false, true),
    PROPOR_ALTERACAO("propor alteração em", true, true),
    CONCORDAR("concordar com a alteração de", false),
    DISCORDAR("discordar da alteração de", true);

    private final String descricao;
    private final boolean exigeMotivo;
    private final boolean comMetodoProprio;
    private final boolean daRotina;

    AcaoProcesso(String descricao, boolean exigeMotivo) {
        this(descricao, exigeMotivo, false, false);
    }

    AcaoProcesso(String descricao, boolean exigeMotivo, boolean comMetodoProprio) {
        this(descricao, exigeMotivo, comMetodoProprio, false);
    }

    AcaoProcesso(String descricao, boolean exigeMotivo, boolean comMetodoProprio, boolean daRotina) {
        this.descricao = descricao;
        this.exigeMotivo = exigeMotivo;
        this.comMetodoProprio = comMetodoProprio;
        this.daRotina = daRotina;
    }

    /**
     * A ação traz dado próprio (identificação da solicitação, protocolo, novo valor e favorecidos) ou tem efeito fora do estado: a raiz tem um
     * método para ela, e o {@code executar} genérico a recusa.
     */
    public boolean comMetodoProprio() {
        return comMetodoProprio;
    }

    /**
     * Ação que só a rotina de pagamento aplica, com o resultado da tesouraria (confirmar, recusar, tentar de novo).
     * Pessoa nenhuma a pede: ela não aparece nas ações disponíveis e o {@code executar} genérico a recusa.
     */
    public boolean daRotina() {
        return daRotina;
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
