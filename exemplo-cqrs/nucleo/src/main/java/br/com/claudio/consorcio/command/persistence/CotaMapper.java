package br.com.claudio.consorcio.command.persistence;

import br.com.claudio.consorcio.command.domain.Cota;
import br.com.claudio.consorcio.shared.persistence.CotaEntity;

final class CotaMapper {

    private CotaMapper() {
    }

    static Cota paraDominio(CotaEntity entity) {
        return Cota.reconstituir(entity.getId(), entity.getNumero(), entity.getValorCredito(), entity.getClienteId());
    }

    static CotaEntity paraEntidade(Cota cota) {
        CotaEntity entity = new CotaEntity();
        entity.setId(cota.id());
        entity.setNumero(cota.numero());
        entity.setValorCredito(cota.valorCredito());
        entity.setClienteId(cota.clienteId());
        return entity;
    }
}
