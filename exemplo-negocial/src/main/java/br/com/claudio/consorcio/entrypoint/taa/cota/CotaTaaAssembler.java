package br.com.claudio.consorcio.entrypoint.taa.cota;

import br.com.claudio.consorcio.entrypoint.taa.PadraoTaa;
import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;

/** DTO Assembler (PoEAA): read model -> contrato do terminal. */
final class CotaTaaAssembler {

    private static final PadraoTaa PADRAO = PadraoTaa.INSTANCIA;

    private CotaTaaAssembler() {
    }

    static CotaTaaResponse montar(CotaDetalhe cota) {
        return new CotaTaaResponse(
                cota.numero(),
                cota.valorCredito(),
                PADRAO.moeda(cota.valorCredito()),
                PADRAO.nome(cota.clienteNome()),
                PADRAO.estadoProcesso(cota.processoAtualEstado()));
    }
}
