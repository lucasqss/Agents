package br.com.claudio.consorcio.persistence.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

/** Mapeamento da tabela processo_proposta_favorecido: favorecido proposto, gravado e lido junto com a proposta. */
@Entity
@Table(name = "processo_proposta_favorecido")
public class ProcessoPropostaFavorecidoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    private int ordem;

    private String nome;

    private String documento;

    private String papel;

    private BigDecimal valor;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public int getOrdem() {
        return ordem;
    }

    public void setOrdem(int ordem) {
        this.ordem = ordem;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getPapel() {
        return papel;
    }

    public void setPapel(String papel) {
        this.papel = papel;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
