package br.com.claudio.consorcio.entrypoint.web;

import static br.com.claudio.consorcio.entrypoint.apresentacao.Formatador.seNaoNulo;

import br.com.claudio.consorcio.entrypoint.apresentacao.Formatador;
import br.com.claudio.consorcio.entrypoint.apresentacao.PadraoApresentacao;
import br.com.claudio.consorcio.entrypoint.apresentacao.Rotulos;
import java.math.BigDecimal;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

/** Padrão do canal web (fictício): rótulos por extenso, "R$ 1.234,56" com espaço comum, CPF visível. */
public final class PadraoWeb implements PadraoApresentacao {

    public static final PadraoWeb INSTANCIA = new PadraoWeb();


    private PadraoWeb() {
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String moeda(@Nullable BigDecimal valor) {
        return seNaoNulo(valor, v -> Formatador.moeda(v, " "));
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String cpf(@Nullable String cpf) {
        return seNaoNulo(cpf, c -> Formatador.cpf(c, "###.###.###-##"));
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String nome(@Nullable String nome) {
        return nome;
    }

    @Override
    public String estadoProcesso(@Nullable String codigo) {
        return Rotulos.estado(codigo);
    }

    @Contract("!null -> !null")
    @Override
    public @Nullable String tipoProcesso(@Nullable String codigo) {
        return seNaoNulo(codigo, Rotulos::tipo);
    }

    @Override
    public String quantidadeCotas(long quantidade) {
        return Formatador.quantidade(quantidade, "cota", "cotas");
    }
}
