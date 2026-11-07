package br.com.claudio.consorcio.canal.web.entrypoint;

/** Linha da lista de clientes do web. Mudanças só aditivas; quebra de contrato vira /web/v2. */
public record ClienteWebResponse(Long id, String nome, String cpf, String cpfFormatado, long quantidadeCotas,
        String quantidadeCotasDescricao) {
}
