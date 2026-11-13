package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.AlteracaoProposta;
import br.com.claudio.consorcio.domain.processo.Documento;
import br.com.claudio.consorcio.domain.processo.Favorecido;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Padrão: Data Mapper [PoEAA].
 * Converte a linha de processo_pagamento (com as de processo_favorecido e a alteração proposta, se houver) no
 * processo do domínio e vice-versa, e cada transição nova do processo numa linha de processo_transicao.
 */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    /** {@code proposta} é nulo quando o processo não aguarda concordância. */
    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity, List<ProcessoFavorecidoEntity> favorecidos,
            AlteracaoProposta proposta) {
        try {
            return ProcessoPagamento.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), Dinheiro.de(entity.getValor()),
                    favorecidosParaDominio(favorecidos),
                    EstadoProcesso.valueOf(entity.getEstado()), entity.getProtocoloPagamento(), proposta,
                    entity.getVersao());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
    }

    static List<Favorecido> favorecidosParaDominio(List<ProcessoFavorecidoEntity> favorecidos) {
        return favorecidos.stream()
                .map(f -> new Favorecido(f.getNome(), Documento.de(f.getDocumento()), f.getPapel(),
                        Dinheiro.de(f.getValor())))
                .toList();
    }

    static AlteracaoProposta propostaParaDominio(ProcessoPropostaEntity proposta,
            List<ProcessoPropostaFavorecidoEntity> favorecidos) {
        return new AlteracaoProposta(Dinheiro.de(proposta.getValor()),
                favorecidos.stream()
                        .map(f -> new Favorecido(f.getNome(), Documento.de(f.getDocumento()), f.getPapel(),
                                Dinheiro.de(f.getValor())))
                        .toList(),
                proposta.getMotivo());
    }

    static ProcessoPropostaEntity propostaParaEntidade(Long processoId, AlteracaoProposta proposta) {
        ProcessoPropostaEntity entity = new ProcessoPropostaEntity();
        entity.setProcessoId(processoId);
        entity.setValor(proposta.valor().valor());
        entity.setMotivo(proposta.motivo());
        return entity;
    }

    static List<ProcessoPropostaFavorecidoEntity> favorecidosDaPropostaParaEntidades(Long processoId,
            AlteracaoProposta proposta) {
        List<Favorecido> favorecidos = proposta.favorecidos();
        return IntStream.range(0, favorecidos.size())
                .mapToObj(i -> {
                    Favorecido favorecido = favorecidos.get(i);
                    ProcessoPropostaFavorecidoEntity entity = new ProcessoPropostaFavorecidoEntity();
                    entity.setProcessoId(processoId);
                    entity.setOrdem(i + 1);
                    entity.setNome(favorecido.nome());
                    entity.setDocumento(favorecido.documento().numero());
                    entity.setPapel(favorecido.papel());
                    entity.setValor(favorecido.parte().valor());
                    return entity;
                })
                .toList();
    }

    /** Uma linha por favorecido, com a ordem (a partir de 1) em que ele foi informado. */
    static List<ProcessoFavorecidoEntity> paraEntidades(Long processoId, List<Favorecido> favorecidos) {
        return IntStream.range(0, favorecidos.size())
                .mapToObj(i -> {
                    Favorecido favorecido = favorecidos.get(i);
                    ProcessoFavorecidoEntity entity = new ProcessoFavorecidoEntity();
                    entity.setProcessoId(processoId);
                    entity.setOrdem(i + 1);
                    entity.setNome(favorecido.nome());
                    entity.setDocumento(favorecido.documento().numero());
                    entity.setPapel(favorecido.papel());
                    entity.setValor(favorecido.parte().valor());
                    return entity;
                })
                .toList();
    }

    static ProcessoTransicaoEntity paraEntidade(Long processoId, Transicao transicao) {
        ProcessoTransicaoEntity entity = new ProcessoTransicaoEntity();
        entity.setProcessoId(processoId);
        entity.setEstadoOrigem(transicao.de().name());
        entity.setEstadoDestino(transicao.para().name());
        entity.setAcao(transicao.acao().name());
        entity.setOcorridoEm(transicao.quando());
        entity.setObservacao(transicao.observacao());
        entity.setValorAnterior(transicao.valorAnterior() == null ? null : transicao.valorAnterior().valor());
        entity.setValorNovo(transicao.valorNovo() == null ? null : transicao.valorNovo().valor());
        return entity;
    }

    static ProcessoPagamentoEntity paraEntidade(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = new ProcessoPagamentoEntity();
        entity.setId(processo.id());
        entity.setCotaId(processo.cotaId());
        entity.setTipo(processo.tipo().name());
        entity.setValor(processo.valor().valor());
        entity.setEstado(processo.estado().name());
        entity.setProtocoloPagamento(processo.protocoloPagamento());
        entity.setVersao(processo.versao());
        return entity;
    }
}
