package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;

/**
 * Padrão: Data Mapper [PoEAA].
 * Converte a linha de processo_pagamento no processo do domínio e vice-versa, e cada transição nova
 * do processo numa linha de processo_transicao.
 */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity) {
        try {
            return ProcessoPagamento.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), Dinheiro.de(entity.getValor()), EstadoProcesso.valueOf(entity.getEstado()),
                    entity.getProtocoloPagamento(), entity.getVersao());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
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
