package br.com.claudio.pagamento.persistence.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import org.jspecify.annotations.Nullable;

/** Mapeamento da tabela processo_pagamento. Uma única entidade para todos os tipos: "tipo" e "estado" são strings. */
@Entity
@Table(name = "processo_pagamento")
public class ProcessoPagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String tipo;

    @Column(name = "numero_cota")
    private String numeroCota;

    private BigDecimal valor;

    private String estado;

    @Version
    private long versao;

    public Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNumeroCota() {
        return numeroCota;
    }

    public void setNumeroCota(String numeroCota) {
        this.numeroCota = numeroCota;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public long getVersao() {
        return versao;
    }

    public void setVersao(long versao) {
        this.versao = versao;
    }
}
