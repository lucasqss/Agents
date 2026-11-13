package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
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
 * Converte a linha de processo_pagamento (com as de processo_favorecido) no processo do domínio e vice-versa,
 * e cada transição nova do processo numa linha de processo_transicao.
 */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity, List<ProcessoFavorecidoEntity> favorecidos) {
        try {
            return ProcessoPagamento.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), Dinheiro.de(entity.getValor()),
                    favorecidos.stream().map(ProcessoPagamentoMapper::paraDominio).toList(),
                    EstadoProcesso.valueOf(entity.getEstado()), entity.getProtocoloPagamento(), entity.getVersao());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
    }

    private static Favorecido paraDominio(ProcessoFavorecidoEntity entity) {
        return new Favorecido(entity.getNome(), Documento.de(entity.getDocumento()), entity.getPapel(),
                Dinheiro.de(entity.getValor()));
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
