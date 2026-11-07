package br.com.claudio.consorcio.shared.apresentacao.taa;

import static br.com.claudio.consorcio.shared.apresentacao.Formatador.seNaoNulo;

import br.com.claudio.consorcio.shared.apresentacao.Formatador;
import br.com.claudio.consorcio.shared.apresentacao.PadraoApresentacao;
import br.com.claudio.consorcio.shared.apresentacao.Rotulos;
import java.math.BigDecimal;

/**
 * Padrão do terminal de autoatendimento (fictício): maiúsculas sem acento (fonte limitada do terminal),
 * nomes com no máximo 20 caracteres, "R$" colado ao valor e CPF quase todo oculto (tela em local público).
 */
public final class PadraoTaa implements PadraoApresentacao {

    public static final PadraoTaa INSTANCIA = new PadraoTaa();

    private static final int MAXIMO_NOME = 20;

    private PadraoTaa() {
    }

    @Override
    public String moeda(BigDecimal valor) {
        return seNaoNulo(valor, v -> Formatador.moeda(v, ""));
    }

    @Override
    public String cpf(String cpf) {
        return seNaoNulo(cpf, c -> Formatador.cpf(c, "***.***.###-##"));
    }

    @Override
    public String nome(String nome) {
        return seNaoNulo(nome, n -> Formatador.limitar(Formatador.maiusculoSemAcento(n), MAXIMO_NOME));
    }

    /** Reaproveita o catálogo base e aplica a regra do terminal. */
    @Override
    public String estadoProcesso(String codigo) {
        return Formatador.maiusculoSemAcento(Rotulos.estado(codigo));
    }

    @Override
    public String tipoProcesso(String codigo) {
        return seNaoNulo(codigo, c -> Formatador.maiusculoSemAcento(Rotulos.tipo(c)));
    }

    @Override
    public String quantidadeCotas(long quantidade) {
        return Formatador.maiusculoSemAcento(Formatador.quantidade(quantidade, "cota", "cotas"));
    }
}
