package br.com.claudio.consorcio.canal.web.entrypoint;

import br.com.claudio.consorcio.canal.web.apresentacao.PadraoWeb;
import br.com.claudio.consorcio.query.application.dto.Pagina;
import br.com.claudio.consorcio.query.persistence.projection.ClienteResumo;
import br.com.claudio.consorcio.query.persistence.projection.CotaDetalhe;
import br.com.claudio.consorcio.query.persistence.projection.ProcessoResumo;
import java.util.List;

/** DTO Assembler (PoEAA): read model -> contrato do canal web. Função pura, sem banco. */
final class WebAssembler {

    private static final PadraoWeb PADRAO = PadraoWeb.INSTANCIA;

    private WebAssembler() {
    }

    static CotaWebResponse cota(CotaDetalhe cota) {
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

    static List<ClienteWebResponse> clientes(List<ClienteResumo> clientes) {
        return clientes.stream()
                .map(c -> new ClienteWebResponse(c.id(), c.nome(), c.cpf(), PADRAO.cpf(c.cpf()), c.quantidadeCotas(),
                        PADRAO.quantidadeCotas(c.quantidadeCotas())))
                .toList();
    }

    static Pagina<ProcessoWebResponse> processos(Pagina<ProcessoResumo> pagina) {
        List<ProcessoWebResponse> itens = pagina.itens().stream()
                .map(p -> new ProcessoWebResponse(p.id(), p.cotaId(), p.cotaNumero(), p.tipo(),
                        PADRAO.tipoProcesso(p.tipo()), p.estado(), PADRAO.estadoProcesso(p.estado()),
                        p.protocoloPagamento()))
                .toList();
        return new Pagina<>(itens, pagina.pagina(), pagina.tamanho(), pagina.total());
    }
}
