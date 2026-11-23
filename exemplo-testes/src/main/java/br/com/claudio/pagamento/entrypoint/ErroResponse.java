package br.com.claudio.pagamento.entrypoint;

import java.util.List;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.jspecify.annotations.Nullable;

/** Formato único de erro da API. {@code campos} só vem preenchido em erro de validação de entrada. */
public record ErroResponse(@Schema(nullable = true) @Nullable String mensagem, List<CampoInvalido> campos) {

    public record CampoInvalido(@Schema(nullable = true) @Nullable String campo, String erro) {
    }

    static ErroResponse de(@Nullable String mensagem) {
        return new ErroResponse(mensagem, List.of());
    }
}
