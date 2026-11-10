package br.com.claudio.consorcio.persistence.processo;

import br.com.claudio.consorcio.domain.processo.ProcessoPagamento;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;

/**
 * Padrão: Data Mapper [PoEAA].
 * Converte a linha única de processo_pagamento no processo do domínio e vice-versa.
 */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity) {
        try {
            return ProcessoPagamento.reconstituir(entity.getId(), entity.getCotaId(),
                    TipoProcesso.valueOf(entity.getTipo()), EstadoProcesso.valueOf(entity.getEstado()),
                    entity.getProtocoloPagamento());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Valor desconhecido no banco: " + e.getMessage(), e);
        }
    }

    static ProcessoPagamentoEntity paraEntidade(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = new ProcessoPagamentoEntity();
        entity.setId(processo.id());
        entity.setCotaId(processo.cotaId());
        entity.setTipo(processo.tipo().name());
        entity.setEstado(processo.estado().name());
        entity.setProtocoloPagamento(processo.protocoloPagamento());
        return entity;
    }
}
