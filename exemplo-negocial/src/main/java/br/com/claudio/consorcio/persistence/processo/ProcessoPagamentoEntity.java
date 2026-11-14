package br.com.claudio.consorcio.persistence.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Mapeamento da tabela processo_pagamento. Uma única entidade para todos os tipos:
 * "tipo" e "estado" são strings, sem herança JPA.
 */
@Entity
@Table(name = "processo_pagamento")
public class ProcessoPagamentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cota_id")
    private Long cotaId;

    private String tipo;

    private BigDecimal valor;

    private String estado;

    @Column(name = "protocolo_pagamento")
    private String protocoloPagamento;

    @Column(name = "identificacao_solicitacao")
    private UUID identificacaoSolicitacao;

    @Version
    private long versao;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCotaId() {
        return cotaId;
    }

    public void setCotaId(Long cotaId) {
        this.cotaId = cotaId;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
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

    public String getProtocoloPagamento() {
        return protocoloPagamento;
    }

    public void setProtocoloPagamento(String protocoloPagamento) {
        this.protocoloPagamento = protocoloPagamento;
    }

    public UUID getIdentificacaoSolicitacao() {
        return identificacaoSolicitacao;
    }

    public void setIdentificacaoSolicitacao(UUID identificacaoSolicitacao) {
        this.identificacaoSolicitacao = identificacaoSolicitacao;
    }

    public long getVersao() {
        return versao;
    }

    public void setVersao(long versao) {
        this.versao = versao;
    }
}
