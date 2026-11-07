package br.com.claudio.consorcio.command.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Regras de negócio que mudam conforme o canal. Fica no domínio, e não no BFF do canal, para que a regra
 * valha igual em qualquer entrada e seja testada sem HTTP. Formato de campo (obrigatório, tamanho) não é
 * isto: esse fica no *Request de cada canal.
 */
public enum CanalDeAtendimento {

    WEB(EnumSet.allOf(TipoProcesso.class)),
    /** O app só abre processo de serviço; imobiliário exige vistoria agendada pelo atendimento. */
    MOBILE(EnumSet.of(TipoProcesso.SERVICO)),
    /** O terminal é só consulta. */
    TAA(EnumSet.noneOf(TipoProcesso.class));

    private final Set<TipoProcesso> abreProcessos;

    CanalDeAtendimento(Set<TipoProcesso> abreProcessos) {
        this.abreProcessos = abreProcessos;
    }

    public static CanalDeAtendimento de(String nome) {
        for (CanalDeAtendimento canal : values()) {
            if (canal.name().equalsIgnoreCase(nome)) {
                return canal;
            }
        }
        throw new DominioException("Canal de atendimento desconhecido: " + nome);
    }

    public void validarAbertura(TipoProcesso tipo) {
        if (!abreProcessos.contains(tipo)) {
            throw new DominioException("O canal " + name() + " não abre processo do tipo " + tipo);
        }
    }
}
