package br.com.claudio.consorcio.entrypoint.mobile;

import static br.com.claudio.consorcio.entrypoint.apresentacao.Formatador.seNaoNulo;

import br.com.claudio.consorcio.entrypoint.apresentacao.Formatador;
import br.com.claudio.consorcio.entrypoint.apresentacao.PadraoApresentacao;
import br.com.claudio.consorcio.entrypoint.apresentacao.Rotulos;
import java.math.BigDecimal;
import java.util.Map;

/** Padrão do canal mobile (fictício): textos curtos para caber em cards, valores abreviados, CPF mascarado. */
public final class PadraoMobile implements PadraoApresentacao {

    public static final PadraoMobile INSTANCIA = new PadraoMobile();

    /** Só o que o mobile abrevia; o resto vem do catálogo base. */
    private static final Map<String, String> ESTADOS_ABREVIADOS = Map.of("AGUARDANDO_VISTORIA", "Aguard. vistoria");
    private static final Map<String, String> TIPOS_ABREVIADOS = Map.of("IMOBILIARIO", "Imóvel");

    private PadraoMobile() {
    }

    @Override
    public String moeda(BigDecimal valor) {
        return seNaoNulo(valor, Formatador::moedaCompacta);
    }

    @Override
    public String cpf(String cpf) {
        return seNaoNulo(cpf, c -> Formatador.cpf(c, "***.###.###-**"));
    }

    @Override
    public String nome(String nome) {
        return seNaoNulo(nome, Formatador::primeiroEUltimoNome);
    }

    @Override
    public String estadoProcesso(String codigo) {
        return codigo == null ? Rotulos.SEM_PROCESSO : ESTADOS_ABREVIADOS.getOrDefault(codigo, Rotulos.estado(codigo));
    }

    @Override
    public String tipoProcesso(String codigo) {
        return seNaoNulo(codigo, c -> TIPOS_ABREVIADOS.getOrDefault(c, Rotulos.tipo(c)));
    }

    @Override
    public String quantidadeCotas(long quantidade) {
        return Formatador.quantidade(quantidade, "cota", "cotas");
    }
}
