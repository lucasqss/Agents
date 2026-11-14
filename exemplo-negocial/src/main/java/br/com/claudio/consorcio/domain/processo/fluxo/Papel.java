package br.com.claudio.consorcio.domain.processo.fluxo;

/**
 * Papel de quem age sobre o processo (quadro "Quem pode executar cada ação", R18). A tabela do fluxo declara, em cada
 * linha, quais papéis a podem pedir. A rotina automática de pagamento não é um papel: ela aplica o resultado da
 * tesouraria e tem autor "sistema" ({@link Transicao#AUTOR_SISTEMA}).
 */
public enum Papel {

    /** O cliente da cota: abre, altera, concorda e discorda, só do que é dele. */
    CLIENTE,
    BACKOFFICE,
    APROVADOR,
    /** A pessoa da tesouraria que solicita o pagamento. */
    TESOURARIA
}
