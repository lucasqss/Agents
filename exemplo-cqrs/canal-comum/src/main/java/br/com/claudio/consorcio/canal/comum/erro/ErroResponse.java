package br.com.claudio.consorcio.canal.comum.erro;

import java.util.List;

/** Formato único de erro da API. {@code campos} só vem preenchido em erro de validação de entrada. */
public record ErroResponse(String mensagem, List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String erro) {
    }

    public static ErroResponse de(String mensagem) {
        return new ErroResponse(mensagem, List.of());
    }
}
