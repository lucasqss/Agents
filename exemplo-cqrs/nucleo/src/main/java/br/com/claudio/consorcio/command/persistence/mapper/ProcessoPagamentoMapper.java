package br.com.claudio.consorcio.command.persistence.mapper;

import br.com.claudio.consorcio.command.domain.entity.pagamento.ProcessoImobiliario;
import br.com.claudio.consorcio.command.domain.entity.pagamento.ProcessoPagamento;
import br.com.claudio.consorcio.command.domain.entity.pagamento.ProcessoServico;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Aberto;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.AguardandoVistoria;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Aprovado;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Cancelado;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.EmAnalise;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.EstadoProcesso;
import br.com.claudio.consorcio.command.domain.entity.pagamento.state.Pago;
import br.com.claudio.consorcio.command.domain.enums.TipoProcesso;
import br.com.claudio.consorcio.shared.persistence.ProcessoPagamentoEntity;

/** Converte a linha única de processo_pagamento na subclasse e no objeto de estado corretos. */
public final class ProcessoPagamentoMapper {

    private ProcessoPagamentoMapper() {
    }

    public static ProcessoPagamento paraDominio(ProcessoPagamentoEntity entity) {
        EstadoProcesso estado = estado(entity.getEstado());
        return switch (TipoProcesso.valueOf(entity.getTipo())) {
            case IMOBILIARIO -> new ProcessoImobiliario(entity.getId(), entity.getCotaId(), estado, entity.getProtocoloPagamento());
            case SERVICO -> new ProcessoServico(entity.getId(), entity.getCotaId(), estado, entity.getProtocoloPagamento());
        };
    }

    public static ProcessoPagamentoEntity paraEntidade(ProcessoPagamento processo) {
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
