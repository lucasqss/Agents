package br.com.claudio.consorcio.query.entrypoint.mobile;

import br.com.claudio.consorcio.query.persistence.ClienteResumo;
import br.com.claudio.consorcio.query.persistence.CotaDetalhe;
import br.com.claudio.consorcio.shared.apresentacao.mobile.PadraoMobile;
import java.util.List;

/**
 * DTO Assembler (PoEAA) do canal mobile. Formata e soma o que já veio da consulta; um número com
 * significado de negócio (ex.: total pago) não nasce aqui, e sim no lado de consulta, para todos os canais.
 */
final class MobileAssembler {

    private static final PadraoMobile PADRAO = PadraoMobile.INSTANCIA;

    private MobileAssembler() {
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

    static ClientesMobileResponse clientes(List<ClienteResumo> clientes) {
        List<ClientesMobileResponse.Item> itens = clientes.stream()
                .map(c -> new ClientesMobileResponse.Item(
                        c.id(),
                        PADRAO.nome(c.nome()),
                        PADRAO.cpf(c.cpf()),
                        c.quantidadeCotas(),
                        PADRAO.quantidadeCotas(c.quantidadeCotas())))
                .toList();
        long total = itens.stream().mapToLong(ClientesMobileResponse.Item::quantidadeCotas).sum();
        return new ClientesMobileResponse(itens, total, PADRAO.quantidadeCotas(total));
    }
}
