package br.com.claudio.consorcio.canal.comum.seguranca;

import br.com.claudio.consorcio.shared.identidade.Solicitante;
import java.util.Optional;

/**
 * Traduz o token do canal para a identidade comum. Cada módulo de canal tem exatamente uma implementação,
 * usando a biblioteca corporativa de segurança daquele canal.
 */
public interface MapeadorDeToken {

    /** Prefixo dos caminhos protegidos pelo token do canal, ex.: "/mobile/". */
    String caminhosProtegidos();

    /** Vazio quando o token não é válido para este canal. */
    Optional<Solicitante> mapear(String token);
}
