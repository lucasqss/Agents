package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.ProcessoImobiliario;
import br.com.claudio.consorcio.command.domain.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.ProcessoServico;
import br.com.claudio.consorcio.command.domain.TipoProcesso;
import br.com.claudio.consorcio.command.domain.estado.Aberto;
import br.com.claudio.consorcio.command.domain.estado.AguardandoVistoria;
import br.com.claudio.consorcio.command.domain.estado.Aprovado;
import br.com.claudio.consorcio.command.domain.estado.Cancelado;
import br.com.claudio.consorcio.command.domain.estado.EmAnalise;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;
import br.com.claudio.consorcio.command.domain.estado.Pago;
import br.com.claudio.consorcio.shared.persistence.ProcessoPagamentoEntity;

/** Converte a linha única de processo_pagamento na subclasse e no objeto de estado corretos. */
final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity) {
        EstadoProcesso estado = estado(entity.getEstado());
        return switch (TipoProcesso.valueOf(entity.getTipo())) {
            case IMOBILIARIO -> new ProcessoImobiliario(entity.getId(), entity.getCotaId(), estado, entity.getProtocoloPagamento());
            case SERVICO -> new ProcessoServico(entity.getId(), entity.getCotaId(), estado, entity.getProtocoloPagamento());
        };
    }

    static ProcessoPagamentoEntity paraEntidade(ProcessoPagamento processo) {
        ProcessoPagamentoEntity entity = new ProcessoPagamentoEntity();
        entity.setId(processo.id());
        entity.setCotaId(processo.cotaId());
        entity.setTipo(processo.tipo().name());
        entity.setEstado(processo.estado().nome());
        entity.setProtocoloPagamento(processo.protocoloPagamento());
        return entity;
    }

    private static EstadoProcesso estado(String nome) {
        return switch (nome) {
            case Aberto.NOME -> new Aberto();
            case EmAnalise.NOME -> new EmAnalise();
            case AguardandoVistoria.NOME -> new AguardandoVistoria();
            case Aprovado.NOME -> new Aprovado();
            case Pago.NOME -> new Pago();
            case Cancelado.NOME -> new Cancelado();
            default -> throw new IllegalStateException("Estado desconhecido no banco: " + nome);
        };
    }
}
