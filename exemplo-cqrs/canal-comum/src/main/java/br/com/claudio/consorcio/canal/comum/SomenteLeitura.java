package br.com.claudio.consorcio.canal.comum;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca o pacote raiz de um canal com escrita bloqueada (no package-info de canal.&lt;canal&gt;).
 * A regra de arquitetura CAN-010 faz o build falhar se alguma classe do canal usar o lado de escrita (command).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PACKAGE)
public @interface SomenteLeitura {
}
