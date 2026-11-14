package br.com.claudio.consorcio.entrypoint.apresentacao;

import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Manual de estilo de um canal. Cada canal (web, mobile, TAA...) tem a sua implementação; ao incluir um
 * formato novo aqui, o código só compila depois que todos os canais decidirem como exibi-lo.
 * Valores nulos devolvem nulo, exceto onde indicado; valor presente sempre devolve texto ({@code @Contract}, conferido
 * pelo NullAway na compilação).
 */
public interface PadraoApresentacao {

    @Contract("!null -> !null")
    @Nullable String moeda(@Nullable BigDecimal valor);

    @Contract("!null -> !null")
    @Nullable String cpf(@Nullable String cpf);

    @Contract("!null -> !null")
    @Nullable String nome(@Nullable String nome);

    /** Rótulo do estado do processo; {@code null} significa "sem processo" e devolve o texto de ausência do canal. */
    String estadoProcesso(@Nullable String codigo);

    @Contract("!null -> !null")
    @Nullable String tipoProcesso(@Nullable String codigo);

    String quantidadeCotas(long quantidade);
}
