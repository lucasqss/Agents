package br.com.claudio.consorcio.canal.web.entrypoint;

import br.com.claudio.consorcio.command.application.dto.DadosCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Contrato HTTP de entrada do cliente. Bean Validation checa o formato (obrigatório, tamanho igual ao da
 * coluna); a regra de negócio continua no domínio.
 */
public record ClienteRequest(
        @NotBlank @Size(max = 200) String nome,
        @NotBlank @Size(max = 14) String cpf) {

    DadosCliente paraDados() {
        return new DadosCliente(nome, cpf);
    }
}
