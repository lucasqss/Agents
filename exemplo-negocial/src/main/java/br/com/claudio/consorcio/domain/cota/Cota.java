package br.com.claudio.consorcio.domain.cota;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;

public class Cota {

    private final Long id;
    private String numero;
    private Dinheiro valorCredito;
    private Long clienteId;

    private Cota(Long id, String numero, Dinheiro valorCredito, Long clienteId) {
        this.id = id;
        alterar(numero, valorCredito, clienteId);
    }

    public static Cota nova(String numero, Dinheiro valorCredito, Long clienteId) {
        return new Cota(null, numero, valorCredito, clienteId);
    }

    public static Cota reconstituir(Long id, String numero, Dinheiro valorCredito, Long clienteId) {
        return new Cota(id, numero, valorCredito, clienteId);
    }

    public void alterar(String numero, Dinheiro valorCredito, Long clienteId) {
        if (numero == null || numero.isBlank()) {
            throw new DominioException("Número da cota é obrigatório");
        }
        if (valorCredito == null || !valorCredito.positivo()) {
            throw new DominioException("Valor do crédito precisa ser positivo");
        }
        if (clienteId == null) {
            throw new DominioException("Cota precisa de um cliente");
        }
        this.numero = numero.strip();
        this.valorCredito = valorCredito;
        this.clienteId = clienteId;
    }

    /** Crédito disponível (R13): o crédito da cota menos o valor dos processos pagos; só pago consome crédito (R14). */
    public Dinheiro creditoDisponivel(Dinheiro totalPago) {
        return valorCredito.subtrair(totalPago);
    }

    /** Recusa o valor que passa do crédito disponível; exatamente o disponível é aceito. */
    public void exigirCreditoPara(Dinheiro valor, Dinheiro totalPago) {
        Dinheiro disponivel = creditoDisponivel(totalPago);
        if (valor.maiorQue(disponivel)) {
            throw new CreditoInsuficienteException("Valor " + valor.valor().toPlainString()
                    + " acima do crédito disponível da cota: " + disponivel.valor().toPlainString());
        }
    }

    public Long id() {
        return id;
    }

    public String numero() {
        return numero;
    }

    public Dinheiro valorCredito() {
        return valorCredito;
    }

    public Long clienteId() {
        return clienteId;
    }
}
