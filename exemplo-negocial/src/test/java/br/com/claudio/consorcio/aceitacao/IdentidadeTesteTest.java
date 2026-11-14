package br.com.claudio.consorcio.aceitacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste.Pessoa;
import io.restassured.path.json.JsonPath;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.PublicKey;
import java.security.Signature;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste unitário, fora do Quarkus, dos tokens que a aceitação envia: o que a aplicação lê do token é o que está aqui
 * (iss, sub, groups, clienteId, exp), e a chave pública de teste confere a assinatura. Não é teste de aceitação.
 */
class IdentidadeTesteTest {

    private static String token(String autorizacao) {
        assertTrue(autorizacao.startsWith("Bearer "), autorizacao);
        return autorizacao.substring("Bearer ".length());
    }

    private static JsonPath payload(String token) {
        return new JsonPath(new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8));
    }

    private static boolean assinaturaConfere(String token, PublicKey chave) throws Exception {
        String[] partes = token.split("\\.");
        Signature assinatura = Signature.getInstance("SHA256withRSA");
        assinatura.initVerify(chave);
        assinatura.update((partes[0] + "." + partes[1]).getBytes(StandardCharsets.US_ASCII));
        return assinatura.verify(Base64.getUrlDecoder().decode(partes[2]));
    }

    @Test
    @DisplayName("Token de pessoa de backoffice: emissor, usuário e papéis, sem clienteId, assinado com a chave de teste")
    void tokenDePessoaDeBackoffice() throws Exception {
        String token = token(IdentidadeTeste.ANA.autorizacao());

        JsonPath claims = payload(token);
        assertEquals("https://exemplo-negocial", claims.getString("iss"));
        assertEquals("ana", claims.getString("sub"));
        assertEquals(List.of("backoffice"), claims.getList("groups"));
        assertNull(claims.get("clienteId"));
        assertTrue(claims.getLong("exp") > Instant.now().getEpochSecond() + 3600);
        assertEquals("RS256", new JsonPath(new String(Base64.getUrlDecoder().decode(token.split("\\.")[0]),
                StandardCharsets.UTF_8)).getString("alg"));
        assertTrue(assinaturaConfere(token, IdentidadeTeste.chavePublica()));
        assertFalse(assinaturaConfere(token, IdentidadeTeste.chavePublicaDeOutroEmissor()));
    }

    @Test
    @DisplayName("Token de cliente: papel cliente e clienteId numérico; pessoa com dois papéis traz os dois")
    void tokenDeClienteETokenComDoisPapeis() {
        JsonPath cliente = payload(token(Pessoa.cliente(42).autorizacao()));
        assertEquals("cliente-42", cliente.getString("sub"));
        assertEquals(List.of("cliente"), cliente.getList("groups"));
        assertEquals(42, cliente.getLong("clienteId"));

        JsonPath carla = payload(token(IdentidadeTeste.CARLA.autorizacao()));
        assertEquals("carla", carla.getString("sub"));
        assertEquals(List.of("aprovador", "backoffice"), carla.getList("groups"));
    }

    @Test
    @DisplayName("Usuário sem papel traz groups vazio")
    void usuarioSemPapel() {
        JsonPath claims = payload(token(IdentidadeTeste.VISITANTE.autorizacao()));
        assertEquals("visitante", claims.getString("sub"));
        assertEquals(List.of(), claims.getList("groups"));
    }

    @Test
    @DisplayName("Tokens que a aplicação deve recusar: outra chave, expirado, outro emissor e texto que não é token")
    void tokensInvalidos() throws Exception {
        String deOutraChave = token(IdentidadeTeste.autorizacaoAssinadaPorOutraChave(IdentidadeTeste.ANA));
        assertFalse(assinaturaConfere(deOutraChave, IdentidadeTeste.chavePublica()));
        assertEquals("https://exemplo-negocial", payload(deOutraChave).getString("iss"));

        String expirado = token(IdentidadeTeste.autorizacaoExpirada(IdentidadeTeste.ANA));
        assertTrue(assinaturaConfere(expirado, IdentidadeTeste.chavePublica()));
        assertTrue(payload(expirado).getLong("exp") < Instant.now().getEpochSecond() - 1800);

        String deOutroEmissor = token(IdentidadeTeste.autorizacaoDeOutroEmissor(IdentidadeTeste.ANA));
        assertTrue(assinaturaConfere(deOutroEmissor, IdentidadeTeste.chavePublica()));
        assertFalse("https://exemplo-negocial".equals(payload(deOutroEmissor).getString("iss")));

        assertEquals(1, IdentidadeTeste.autorizacaoMalformada().split("\\.").length);
    }

    @Test
    @DisplayName("A configuração entrega a chave pública de teste em arquivo e o emissor esperado")
    void configuracaoDaAplicacao() throws Exception {
        Map<String, String> configuracao = IdentidadeTeste.configuracaoDaAplicacao();

        assertEquals("https://exemplo-negocial", configuracao.get("mp.jwt.verify.issuer"));
        Path arquivo = Path.of(configuracao.get("mp.jwt.verify.publickey.location"));
        assertTrue(arquivo.isAbsolute());
        assertTrue(Files.readString(arquivo).startsWith("-----BEGIN PUBLIC KEY-----"));
    }
}
