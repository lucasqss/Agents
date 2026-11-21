package br.com.claudio.consorcio.persistence.cliente.projection;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "ClienteResumo")
public record ClienteProjection(Long id, String nome, String cpf, Long quantidadeCotas) {
}
