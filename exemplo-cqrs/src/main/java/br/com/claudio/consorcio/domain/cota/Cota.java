package br.com.claudio.consorcio.domain.cota;

import br.com.claudio.consorcio.domain.DominioException;
import java.math.BigDecimal;

public class Cota {

    private final Long id;
    private String numero;
    private BigDecimal valorCredito;
    private Long clienteId;

    private Cota(Long id, String numero, BigDecimal valorCredito, Long clienteId) {
        this.id = id;
        alterar(numero, valorCredito, clienteId);
    }

    public static Cota nova(String numero, BigDecimal valorCredito, Long clienteId) {
        return new Cota(null, numero, valorCredito, clienteId);
    }

    public static Cota reconstituir(Long id, String numero, BigDecimal valorCredito, Long clienteId) {
        return new Cota(id, numero, valorCredito, clienteId);
    }

    public void alterar(String numero, BigDecimal valorCredito, Long clienteId) {
        if (numero == null || numero.isBlank()) {
            throw new DominioException("Número da cota é obrigatório");
        }
        if (valorCredito == null || valorCredito.signum() <= 0) {
            throw new DominioException("Valor do crédito precisa ser positivo");
        }
        if (clienteId == null) {
            throw new DominioException("Cota precisa de um cliente");
        }
        this.numero = numero.strip();
        this.valorCredito = valorCredito;
        this.clienteId = clienteId;
    }

    public Long id() {
        return id;
    }

    public String numero() {
        return numero;
    }

    public BigDecimal valorCredito() {
        return valorCredito;
    }

    public Long clienteId() {
        return clienteId;
    }
}
