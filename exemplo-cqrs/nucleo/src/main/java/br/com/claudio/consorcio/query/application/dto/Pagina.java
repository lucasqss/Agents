package br.com.claudio.consorcio.query.application.dto;

import java.util.List;

/** Página de resultados; {@code pagina} começa em 1. */
public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {
}
