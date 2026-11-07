package br.com.claudio.consorcio.canal.comum.apresentacao;

import br.com.claudio.consorcio.command.domain.TipoProcesso;
import br.com.claudio.consorcio.command.domain.estado.EstadoProcesso;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** O catálogo base de rótulos acompanha o domínio. Os padrões de cada canal são testados no módulo do canal. */
class RotulosTest {

    @Test
    void todoEstadoETipoDoDominioTemRotulo() throws ReflectiveOperationException {
        // Um estado novo no domínio sem rótulo apareceria cru ("EM_REVISAO") na tela dos canais.
        for (Class<?> estado : EstadoProcesso.class.getPermittedSubclasses()) {
            String codigo = (String) estado.getField("NOME").get(null);
            Assertions.assertTrue(Rotulos.temEstado(codigo), "estado sem rótulo: " + codigo);
        }
        for (TipoProcesso tipo : TipoProcesso.values()) {
            Assertions.assertTrue(Rotulos.temTipo(tipo.name()), "tipo sem rótulo: " + tipo);
        }
    }
}
