package br.com.claudio.consorcio.entrypoint.taa;

import static br.com.claudio.consorcio.entrypoint.apresentacao.Formatador.seNaoNulo;

import br.com.claudio.consorcio.entrypoint.apresentacao.Formatador;
import br.com.claudio.consorcio.entrypoint.apresentacao.PadraoApresentacao;
import br.com.claudio.consorcio.entrypoint.apresentacao.Rotulos;
import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/**
 * Padrão do terminal de autoatendimento (fictício): maiúsculas sem acento (fonte limitada do terminal),
 * nomes com no máximo 20 caracteres, "R$" colado ao valor e CPF quase todo oculto (tela em local público).
 */
public final class PadraoTaa implements PadraoApresentacao {

    public static final PadraoTaa INSTANCIA = new PadraoTaa();

    private static final int MAXIMO_NOME = 20;

    private PadraoTaa() {
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String moeda(@Nullable BigDecimal valor) {
        return seNaoNulo(valor, v -> Formatador.moeda(v, ""));
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String cpf(@Nullable String cpf) {
        return seNaoNulo(cpf, c -> Formatador.cpf(c, "***.***.###-##"));
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String nome(@Nullable String nome) {
        return seNaoNulo(nome, n -> Formatador.limitar(Formatador.maiusculoSemAcento(n), MAXIMO_NOME));
    }

    /** Reaproveita o catálogo base e aplica a regra do terminal. */
    @Override
    public String estadoProcesso(@Nullable String codigo) {
        return Formatador.maiusculoSemAcento(Rotulos.estado(codigo));
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String tipoProcesso(@Nullable String codigo) {
        return seNaoNulo(codigo, c -> Formatador.maiusculoSemAcento(Rotulos.tipo(c)));
    }

    @Override
    public String quantidadeCotas(long quantidade) {
        return Formatador.maiusculoSemAcento(Formatador.quantidade(quantidade, "cota", "cotas"));
    }
}
