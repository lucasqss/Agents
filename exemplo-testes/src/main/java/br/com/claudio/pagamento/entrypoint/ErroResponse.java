package br.com.claudio.pagamento.entrypoint;

import java.util.List;
import org.jspecify.annotations.Nullable;

/** Formato único de erro da API. {@code campos} só vem preenchido em erro de validação de entrada. */
public record ErroResponse(@Nullable String mensagem, List<CampoInvalido> campos) {

    public record CampoInvalido(@Nullable String campo, String erro) {
    }

    static ErroResponse de(@Nullable String mensagem) {
        return new ErroResponse(mensagem, List.of());
    }
}
