package br.com.claudio.consorcio.entrypoint.web;

import static br.com.claudio.consorcio.entrypoint.apresentacao.Formatador.seNaoNulo;

import br.com.claudio.consorcio.entrypoint.apresentacao.Formatador;
import br.com.claudio.consorcio.entrypoint.apresentacao.PadraoApresentacao;
import br.com.claudio.consorcio.entrypoint.apresentacao.Rotulos;
import java.math.BigDecimal;

/** Padrão do canal web (fictício): rótulos por extenso, "R$ 1.234,56" com espaço comum, CPF visível. */
public final class PadraoWeb implements PadraoApresentacao {

    public static final PadraoWeb INSTANCIA = new PadraoWeb();


    private PadraoWeb() {
    }

    @Override
    public String moeda(BigDecimal valor) {
        return seNaoNulo(valor, v -> Formatador.moeda(v, " "));
    }

    @Override
    public String cpf(String cpf) {
        return seNaoNulo(cpf, c -> Formatador.cpf(c, "###.###.###-##"));
    }

    @Override
    public String nome(String nome) {
        return nome;
    }

    @Override
    public String estadoProcesso(String codigo) {
        return Rotulos.estado(codigo);
    }

    @Override
    public String tipoProcesso(String codigo) {
        return seNaoNulo(codigo, Rotulos::tipo);
    }

    @Override
    public String quantidadeCotas(long quantidade) {
        return Formatador.quantidade(quantidade, "cota", "cotas");
    }
}
