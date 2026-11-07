package br.com.claudio.consorcio.command.domain.enums;

import br.com.claudio.consorcio.command.domain.exception.DominioException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Regra que muda por canal: testada no domínio, sem HTTP e sem saber como cada canal autentica. */
class CanalDeAtendimentoTest {

    @Test
    void webAbreQualquerProcesso() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            Assertions.assertDoesNotThrow(() -> CanalDeAtendimento.WEB.validarAbertura(tipo));
        }
    }

    @Test
    void mobileSoAbreServico() {
        Assertions.assertDoesNotThrow(() -> CanalDeAtendimento.MOBILE.validarAbertura(TipoProcesso.SERVICO));
        DominioException erro = Assertions.assertThrows(DominioException.class,
                () -> CanalDeAtendimento.MOBILE.validarAbertura(TipoProcesso.IMOBILIARIO));
        Assertions.assertEquals("O canal MOBILE não abre processo do tipo IMOBILIARIO", erro.getMessage());
    }

    @Test
    void taaNaoAbreProcesso() {
        for (TipoProcesso tipo : TipoProcesso.values()) {
            Assertions.assertThrows(DominioException.class, () -> CanalDeAtendimento.TAA.validarAbertura(tipo));
        }
    }

    @Test
    void canalPeloNome() {
        Assertions.assertEquals(CanalDeAtendimento.MOBILE, CanalDeAtendimento.de("mobile"));
        Assertions.assertThrows(DominioException.class, () -> CanalDeAtendimento.de("fax"));
    }
}
