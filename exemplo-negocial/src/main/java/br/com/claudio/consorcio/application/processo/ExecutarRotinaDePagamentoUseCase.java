package br.com.claudio.consorcio.application.processo;

import br.com.claudio.consorcio.domain.processo.IdentificacaoSolicitacao;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamentoRepository;
import br.com.claudio.consorcio.domain.processo.ResultadoPagamento;
import br.com.claudio.consorcio.gateway.tesouraria.PedidoDePagamento;
import br.com.claudio.consorcio.gateway.tesouraria.RespostaDaTesouraria;
import br.com.claudio.consorcio.gateway.tesouraria.TesourariaGateway;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import org.jboss.logging.Logger;

/**
 * Padrão: Service Layer [PoEAA]. Passo do meio da saga de pagamento (ADR 002), chamado pelo job agendado: para cada
 * processo em Pagamento solicitado, lê o processo, chama a tesouraria e entrega o resultado a
 * {@link AplicarResultadoPagamentoUseCase}. Esta classe NÃO é transacional, de propósito: a chamada externa acontece
 * sem transação aberta nem bloqueio no banco (a tesouraria lenta não prende conexão), e a gravação é a transação curta do
 * outro caso de uso. A identificação da solicitação, a mesma em todas as tentativas, é a chave de idempotência: se a
 * tesouraria fez a ordem e a gravação não aconteceu, a próxima rodada recebe ALREADY_PROCESSED com o protocolo.
 * Falha em um processo não impede os demais; duas execuções ao mesmo tempo (ou em duas instâncias) chamam com a mesma
 * identificação e só a primeira a gravar aplica o resultado.
 */
@ApplicationScoped
public class ExecutarRotinaDePagamentoUseCase {

    private static final Logger LOG = Logger.getLogger(ExecutarRotinaDePagamentoUseCase.class);

    private final ProcessoPagamentoRepository processos;
    private final TesourariaGateway tesouraria;
    private final AplicarResultadoPagamentoUseCase aplicarResultado;

    public ExecutarRotinaDePagamentoUseCase(ProcessoPagamentoRepository processos, TesourariaGateway tesouraria,
            AplicarResultadoPagamentoUseCase aplicarResultado) {
        this.processos = processos;
        this.tesouraria = tesouraria;
        this.aplicarResultado = aplicarResultado;
    }

    public void executar() {
        for (Long id : processos.idsEmPagamentoSolicitado()) {
            try {
                tentarPagamento(id);
            } catch (RuntimeException e) {
                LOG.errorf(e, "Rotina de pagamento: falha no processo %d; ele segue em pagamento solicitado", id);
            }
        }
    }

    private void tentarPagamento(Long id) {
        ProcessoPagamento processo = processos.buscarPorId(id).orElse(null);
        if (processo == null) {
            return;
        }
        IdentificacaoSolicitacao identificacao = processo.identificacaoSolicitacao().orElse(null);
        if (identificacao == null) {
            return; // saiu de Pagamento solicitado depois da listagem
        }
        RespostaDaTesouraria resposta = tesouraria.solicitarPagamento(pedido(processo, identificacao));
        aplicarResultado.executar(id, identificacao, traduzir(resposta));
    }

    private static PedidoDePagamento pedido(ProcessoPagamento processo, IdentificacaoSolicitacao identificacao) {
        List<PedidoDePagamento.Parte> partes = processo.favorecidos().stream()
                .map(f -> new PedidoDePagamento.Parte(f.nome(), f.documento().numero(), f.papel(), f.parte().valor()))
                .toList();
        return new PedidoDePagamento(identificacao.texto(), processo.id(), processo.valor().valor(), partes);
    }

    private static ResultadoPagamento traduzir(RespostaDaTesouraria resposta) {
        return switch (resposta) {
            case RespostaDaTesouraria.Confirmada confirmada -> ResultadoPagamento.confirmado(confirmada.protocolo());
            case RespostaDaTesouraria.Recusada recusada -> ResultadoPagamento.recusado(recusada.motivo());
            case RespostaDaTesouraria.SemResposta semResposta ->
                    new ResultadoPagamento.SemResposta(semResposta.descricao());
            case RespostaDaTesouraria.Incoerente incoerente -> new ResultadoPagamento.Incoerente(incoerente.descricao());
        };
    }
}
