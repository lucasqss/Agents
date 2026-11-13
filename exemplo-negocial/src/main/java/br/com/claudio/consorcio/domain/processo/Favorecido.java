package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;

/**
 * Padrão: Value Object [PoEAA]. Quem recebe uma parte do valor do processo (R15, R29): identificado
 * por nome, documento e papel, todos obrigatórios; a parte é maior que zero. Não tem identidade própria:
 * pertence ao {@link ProcessoPagamento}, que protege a soma e a repetição.
 */
public record Favorecido(String nome, Documento documento, String papel, Dinheiro parte) {

    public Favorecido {
        if (nome == null || nome.isBlank()) {
            throw new DominioException("Nome do favorecido é obrigatório");
        }
        if (documento == null) {
            throw new DominioException("Documento do favorecido é obrigatório");
        }
        if (papel == null || papel.isBlank()) {
            throw new DominioException("Papel do favorecido é obrigatório");
        }
        if (parte == null || !parte.positivo()) {
            throw new DominioException("Parte do favorecido " + nome + " precisa ser maior que zero");
        }
    }
}
