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
