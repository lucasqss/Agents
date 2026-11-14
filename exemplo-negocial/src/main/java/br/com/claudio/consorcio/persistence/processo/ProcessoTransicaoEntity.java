package br.com.claudio.consorcio.persistence.processo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

/** Mapeamento da tabela processo_transicao: o histórico de mudanças de estado, só com inclusões. */
@Entity
@Table(name = "processo_transicao")
public class ProcessoTransicaoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "processo_id")
    private Long processoId;

    @Column(name = "estado_origem")
    private String estadoOrigem;

    @Column(name = "estado_destino")
    private String estadoDestino;

    private String acao;

    @Column(name = "ocorrido_em")
    private Instant ocorridoEm;

    private String autor;

    private @Nullable String observacao;

    @Column(name = "valor_anterior")
    private @Nullable BigDecimal valorAnterior;

    @Column(name = "valor_novo")
    private @Nullable BigDecimal valorNovo;

    public Long getId() {
        return id;
    }

    public void setId(@Nullable Long id) {
        this.id = id;
    }

    public Long getProcessoId() {
        return processoId;
    }

    public void setProcessoId(Long processoId) {
        this.processoId = processoId;
    }

    public String getEstadoOrigem() {
        return estadoOrigem;
    }

    public void setEstadoOrigem(String estadoOrigem) {
        this.estadoOrigem = estadoOrigem;
    }

    public String getEstadoDestino() {
        return estadoDestino;
    }

    public void setEstadoDestino(String estadoDestino) {
        this.estadoDestino = estadoDestino;
    }

    public String getAcao() {
        return acao;
    }

    public void setAcao(String acao) {
        this.acao = acao;
    }

    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    public void setOcorridoEm(Instant ocorridoEm) {
        this.ocorridoEm = ocorridoEm;
    }

    public String getAutor() {
        return autor;
    }

    public void setAutor(String autor) {
        this.autor = autor;
    }

    public @Nullable String getObservacao() {
        return observacao;
    }

    public void setObservacao(@Nullable String observacao) {
        this.observacao = observacao;
    }

    public @Nullable BigDecimal getValorAnterior() {
        return valorAnterior;
    }

    public void setValorAnterior(@Nullable BigDecimal valorAnterior) {
        this.valorAnterior = valorAnterior;
    }

    public @Nullable BigDecimal getValorNovo() {
        return valorNovo;
    }

    public void setValorNovo(@Nullable BigDecimal valorNovo) {
        this.valorNovo = valorNovo;
    }
}
