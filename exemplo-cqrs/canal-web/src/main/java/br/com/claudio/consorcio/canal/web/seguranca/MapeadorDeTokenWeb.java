package br.com.claudio.consorcio.canal.web.seguranca;

import br.com.claudio.consorcio.canal.comum.seguranca.MapeadorDeToken;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Token do web -> identidade comum. SIMULAÇÃO: na empresa, quem valida o token é a biblioteca corporativa
 * de segurança do web (dependência só deste módulo). Formato fictício: "web:&lt;matrícula&gt;".
 */
@ApplicationScoped
public class MapeadorDeTokenWeb implements MapeadorDeToken {

    private static final String PREFIXO = "web:";

    @Override
    public String caminhosProtegidos() {
        return "/web/";
    }

    @Override
    public Optional<Solicitante> mapear(String token) {
        if (!token.startsWith(PREFIXO) || token.length() == PREFIXO.length()) {
            return Optional.empty();
        }
        return Optional.of(new Solicitante(token.substring(PREFIXO.length()), "WEB"));
    }
}
