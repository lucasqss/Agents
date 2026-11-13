package br.com.claudio.consorcio.entrypoint.mobile.cliente;

import br.com.claudio.consorcio.entrypoint.mobile.PadraoMobile;
import br.com.claudio.consorcio.persistence.cliente.ClienteResumo;
import java.util.List;

/**
 * DTO Assembler (PoEAA) do canal mobile. Formata e soma o que já veio da consulta; um número com
 * significado de negócio (ex.: total pago) não nasce aqui, e sim na consulta, para todos os canais.
 */
final class ClienteMobileAssembler {

    private static final PadraoMobile PADRAO = PadraoMobile.INSTANCIA;

    private ClienteMobileAssembler() {
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
