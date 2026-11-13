package br.com.claudio.consorcio.entrypoint.web.cota;

import br.com.claudio.consorcio.entrypoint.web.PadraoWeb;
import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;

/** DTO Assembler (PoEAA): read model -> contrato do canal web. Função pura, sem banco. */
final class CotaWebAssembler {

    private static final PadraoWeb PADRAO = PadraoWeb.INSTANCIA;

    private CotaWebAssembler() {
    }

    static CotaWebResponse montar(CotaDetalhe cota) {
        CotaWebResponse.ProcessoAtual processo = cota.processoAtualId() == null ? null
                : new CotaWebResponse.ProcessoAtual(
                        cota.processoAtualId(),
                        cota.processoAtualTipo(),
                        PADRAO.tipoProcesso(cota.processoAtualTipo()),
                        cota.processoAtualEstado(),
                        PADRAO.estadoProcesso(cota.processoAtualEstado()));
        return new CotaWebResponse(
                cota.id(),
                cota.numero(),
                cota.valorCredito(),
                PADRAO.moeda(cota.valorCredito()),
                cota.clienteId(),
                PADRAO.nome(cota.clienteNome()),
                processo);
    }
}
