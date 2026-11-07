package br.com.claudio.consorcio.canal.taa.seguranca;

import br.com.claudio.consorcio.canal.comum.seguranca.MapeadorDeToken;
import br.com.claudio.consorcio.shared.identidade.Solicitante;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;

/**
 * Token do terminal -> identidade comum. SIMULAÇÃO: na empresa, quem valida é a biblioteca corporativa do
 * TAA (certificado do equipamento). Formato fictício: "taa-&lt;código do terminal&gt;".
 */
@ApplicationScoped
public class MapeadorDeTokenTaa implements MapeadorDeToken {

    private static final String PREFIXO = "taa-";

    @Override
    public String caminhosProtegidos() {
        return "/taa/";
    }

    @Override
    public Optional<Solicitante> mapear(String token) {
        if (!token.startsWith(PREFIXO) || token.length() == PREFIXO.length()) {
            return Optional.empty();
        }
        return Optional.of(new Solicitante(token.substring(PREFIXO.length()), "TAA"));
    }
}
