package br.com.claudio.consorcio.persistence.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Mapeamento da tabela processo_alteracao_proposta: a alteração à espera do cliente, no máximo uma por processo. */
@Entity
@Table(name = "processo_alteracao_proposta")
public class ProcessoPropostaEntity {

    @Id
    @Column(name = "processo_id")
    private Long processoId;

    private BigDecimal valor;

    private String motivo;

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
