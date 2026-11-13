package br.com.claudio.consorcio.entrypoint.mobile.cota;

import br.com.claudio.consorcio.entrypoint.mobile.PadraoMobile;
import br.com.claudio.consorcio.persistence.cota.CotaDetalhe;

/**
 * DTO Assembler (PoEAA) do canal mobile. Formata o que já veio da consulta; um número com
 * significado de negócio (ex.: total pago) não nasce aqui, e sim na consulta, para todos os canais.
 */
final class CotaMobileAssembler {

    private static final PadraoMobile PADRAO = PadraoMobile.INSTANCIA;

    private CotaMobileAssembler() {
    }

    static CotaMobileResponse cota(CotaDetalhe cota) {
        return new CotaMobileResponse(
                cota.id(),
                cota.numero(),
                cota.valorCredito(),
                PADRAO.moeda(cota.valorCredito()),
                PADRAO.nome(cota.clienteNome()),
                cota.processoAtualEstado(),
                PADRAO.estadoProcesso(cota.processoAtualEstado()));
    }
}
