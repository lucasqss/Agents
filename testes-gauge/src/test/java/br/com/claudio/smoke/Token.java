package br.com.claudio.smoke;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;

/**
 * Gera um token JWT (RS256) igual ao de {@code exemplo-negocial/scripts/token-dev.sh}: assinado com a chave privada de
 * DESENVOLVIMENTO, que a aplicação do docker-compose aceita. O caminho da chave vem de {@code CHAVE_PRIVADA_JWT}.
 */
final class Token {

    private static final String EMISSOR = System.getenv().getOrDefault("EMISSOR_JWT", "https://exemplo-negocial");

    private Token() {
    }

    static String para(String usuario, String papel) {
        long agora = Instant.now().getEpochSecond();
        String cabecalho = "{\"alg\":\"RS256\",\"typ\":\"JWT\"}";
        String corpo = "{\"iss\":\"" + EMISSOR + "\",\"sub\":\"" + usuario + "\",\"upn\":\"" + usuario
                + "\",\"groups\":[\"" + papel + "\"],\"iat\":" + agora + ",\"exp\":" + (agora + 3600) + "}";
        String assinado = base64(cabecalho.getBytes(StandardCharsets.UTF_8)) + "."
                + base64(corpo.getBytes(StandardCharsets.UTF_8));
        try {
            Signature assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initSign(chave());
            assinatura.update(assinado.getBytes(StandardCharsets.US_ASCII));
            return assinado + "." + base64(assinatura.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o token", e);
        }
    }

    private static PrivateKey chave() throws GeneralSecurityException {
        String caminho = System.getenv("CHAVE_PRIVADA_JWT");
        if (caminho == null || caminho.isBlank()) {
            throw new IllegalStateException("Defina CHAVE_PRIVADA_JWT com o caminho da chave privada de desenvolvimento");
        }
        try {
            String pem = Files.readString(Path.of(caminho));
            String base64 = pem.replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(base64)));
        } catch (IOException e) {
            throw new UncheckedIOException("Chave privada não encontrada em " + caminho, e);
        }
    }

    private static String base64(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
