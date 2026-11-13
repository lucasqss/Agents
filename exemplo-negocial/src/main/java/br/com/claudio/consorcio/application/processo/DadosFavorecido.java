package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.Documento;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import java.math.BigDecimal;

/** Um favorecido do pedido (abertura, alteração ou proposta): quem recebe e quanto (R15, R29). */
public record DadosFavorecido(String nome, String documento, String papel, BigDecimal valor) {

    Favorecido paraFavorecido() {
        return new Favorecido(nome, Documento.de(documento), papel, Dinheiro.de(valor));
    }
}
