package br.com.claudio.consorcio.entrypoint.rest.cota;

import br.com.claudio.consorcio.application.cota.DadosCota;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Contrato HTTP de entrada da cota; tamanhos iguais aos das colunas. */
public record CotaRequest(
        @NotBlank @Size(max = 30) String numero,
        @NotNull @Positive BigDecimal valorCredito,
        @NotNull Long clienteId) {

    DadosCota paraDados() {
        return new DadosCota(numero, valorCredito, clienteId);
    }
}
