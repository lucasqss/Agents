package br.com.claudio.consorcio.canal.comum.apresentacao;

import java.math.BigDecimal;

/**
 * Manual de estilo de um canal. Cada canal (web, mobile, TAA...) tem a sua implementação; ao incluir um
 * formato novo aqui, o código só compila depois que todos os canais decidirem como exibi-lo.
 * Valores nulos devolvem nulo, exceto onde indicado.
 */
public interface PadraoApresentacao {

    String moeda(BigDecimal valor);

    String cpf(String cpf);

    String nome(String nome);

    /** Rótulo do estado do processo; {@code null} significa "sem processo" e devolve o texto de ausência do canal. */
    String estadoProcesso(String codigo);

    String tipoProcesso(String codigo);

    String quantidadeCotas(long quantidade);
}
