package br.com.claudio.consorcio.persistence.cota;

import br.com.claudio.consorcio.domain.cota.Cota;

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
