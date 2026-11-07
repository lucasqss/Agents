package br.com.claudio.consorcio.canal.mobile.entrypoint;

import br.com.claudio.consorcio.command.domain.TipoProcesso;
import jakarta.validation.constraints.NotNull;

/**
 * Pedido de abertura de processo pelo app. Aqui só o formato; quais tipos o app pode abrir é regra de negócio
 * por canal e fica no domínio (CanalDeAtendimento).
 */
public record NovoProcessoMobileRequest(@NotNull Long cotaId, @NotNull TipoProcesso tipo) {
}
