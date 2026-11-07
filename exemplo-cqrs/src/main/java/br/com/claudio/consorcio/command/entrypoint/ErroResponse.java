package br.com.claudio.consorcio.command.entrypoint;

import java.util.List;

/** Formato único de erro da API. {@code campos} só vem preenchido em erro de validação de entrada. */
public record ErroResponse(String mensagem, List<CampoInvalido> campos) {

    public record CampoInvalido(String campo, String erro) {
    }

    static ErroResponse de(String mensagem) {
        return new ErroResponse(mensagem, List.of());
    }
}
