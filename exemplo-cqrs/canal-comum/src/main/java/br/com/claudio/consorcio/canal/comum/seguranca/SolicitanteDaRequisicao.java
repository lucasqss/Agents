package br.com.claudio.consorcio.canal.comum.seguranca;

import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.RequestScoped;

/** Quem fez a requisição atual, preenchido pelo FiltroDeToken. Os Resources passam isto ao use case. */
@RequestScoped
public class SolicitanteDaRequisicao {

    private Solicitante solicitante;

    void definir(Solicitante solicitante) {
        this.solicitante = solicitante;
    }

    public Solicitante atual() {
        if (solicitante == null) {
            throw new IllegalStateException("Requisição sem solicitante: o caminho não é protegido pelo token do canal");
        }
        return solicitante;
    }
}
