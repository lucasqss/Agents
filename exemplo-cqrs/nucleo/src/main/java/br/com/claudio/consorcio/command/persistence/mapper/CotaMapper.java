package br.com.claudio.consorcio.command.persistence.mapper;

import br.com.claudio.consorcio.command.domain.entity.Cota;
import br.com.claudio.consorcio.shared.persistence.CotaEntity;

public final class CotaMapper {

    private CotaMapper() {
    }

    public static Cota paraDominio(CotaEntity entity) {
        return Cota.reconstituir(entity.getId(), entity.getNumero(), entity.getValorCredito(), entity.getClienteId());
    }

    public static CotaEntity paraEntidade(Cota cota) {
        CotaEntity entity = new CotaEntity();
        entity.setId(cota.id());
        entity.setNumero(cota.numero());
        entity.setValorCredito(cota.valorCredito());
        entity.setClienteId(cota.clienteId());
        return entity;
    }
}
