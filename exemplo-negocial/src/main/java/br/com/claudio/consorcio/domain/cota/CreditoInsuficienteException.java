package br.com.claudio.consorcio.domain.cota;

import br.com.claudio.consorcio.domain.DominioException;

/** O valor pedido passa do crédito disponível da cota (R13); é um conflito com o estado da cota, não dado malformado. */
public class CreditoInsuficienteException extends DominioException {

    public CreditoInsuficienteException(String mensagem) {
        super(mensagem);
    }
}
