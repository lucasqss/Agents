package br.com.claudio.consorcio.entrypoint.mobile.cliente;

import java.util.List;

/** Lista de clientes do app, com o total de cotas somado para o rodapé da tela. */
public record ClientesMobileResponse(List<Item> itens, long totalCotas, String totalCotasDescricao) {

    public record Item(Long id, String nome, String cpf, long quantidadeCotas, String quantidadeCotasDescricao) {
    }
}
