package br.com.claudio.consorcio.canal.mobile.seguranca;

import br.com.claudio.consorcio.canal.comum.seguranca.MapeadorDeToken;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Token do app -> identidade comum. SIMULAÇÃO: na empresa, quem valida o token é a biblioteca corporativa
 * de segurança do mobile (dependência só deste módulo). Formato fictício: "mob.&lt;cpf com 11 dígitos&gt;".
 */
@ApplicationScoped
public class MapeadorDeTokenMobile implements MapeadorDeToken {

    private static final String PREFIXO = "mob.";

    @Override
    public String caminhosProtegidos() {
        return "/mobile/";
    }

    @Override
    public Optional<Solicitante> mapear(String token) {
        if (!token.startsWith(PREFIXO)) {
            return Optional.empty();
        }
        String cpf = token.substring(PREFIXO.length());
        return cpf.matches("\\d{11}") ? Optional.of(new Solicitante(cpf, "MOBILE")) : Optional.empty();
    }
}
