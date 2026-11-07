package br.com.claudio.consorcio.canal.taa.entrypoint;

import br.com.claudio.consorcio.query.persistence.CotaDetalhe;
import br.com.claudio.consorcio.canal.taa.apresentacao.PadraoTaa;

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
