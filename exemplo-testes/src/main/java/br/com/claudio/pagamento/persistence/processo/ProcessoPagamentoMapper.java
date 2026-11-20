package br.com.claudio.pagamento.persistence.processo;

import br.com.claudio.pagamento.domain.Dinheiro;
import br.com.claudio.pagamento.domain.DominioException;
import br.com.claudio.pagamento.domain.processo.ProcessoPagamento;
import br.com.claudio.pagamento.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.pagamento.domain.processo.fluxo.Transicao;
import br.com.claudio.pagamento.domain.processo.tipo.TipoProcesso;

/**
 * Padrão: Data Mapper [PoEAA]. Converte a linha de processo_pagamento no processo do domínio e vice-versa, e cada
 * transição nova numa linha de processo_transicao.
 */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity) {
        try {
            return ProcessoPagamento.reconstituir(entity.getId(), TipoProcesso.valueOf(entity.getTipo()),
                    entity.getNumeroCota(), Dinheiro.de(entity.getValor()), EstadoProcesso.valueOf(entity.getEstado()),
                    entity.getVersao());
        } catch (IllegalArgumentException | DominioException e) {
            throw new IllegalStateException("Valor desconhecido no banco: processo " + entity.getId() + " inválido: "
                    + e.getMessage(), e);
        }
    }

    static ProcessoPagamentoEntity paraEntidade(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = new ProcessoPagamentoEntity();
        entity.setId(processo.id());
        entity.setTipo(processo.tipo().name());
        entity.setNumeroCota(processo.numeroCota());
        entity.setValor(processo.valor().valor());
        entity.setEstado(processo.estado().name());
        entity.setVersao(processo.versao());
        return entity;
    }

    static ProcessoTransicaoEntity paraEntidade(Long processoId, Transicao transicao) {
        ProcessoTransicaoEntity entity = new ProcessoTransicaoEntity();
        entity.setProcessoId(processoId);
        entity.setEstadoOrigem(transicao.de().name());
        entity.setEstadoDestino(transicao.para().name());
        entity.setAcao(transicao.acao().name());
        entity.setOcorridoEm(transicao.quando());
        entity.setAutor(transicao.autor());
        entity.setObservacao(transicao.observacao());
        return entity;
    }
}
