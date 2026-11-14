package br.com.claudio.consorcio.aceitacao;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Padrão: Test Utility Method [xUnit Test Patterns].
 * Identidade dos testes de aceitação (R18, R19): gera os tokens JWT que a aplicação de teste aceita e os que ela
 * deve recusar. Único ponto que conhece o contrato de identidade decidido por quem coordena:
 * {@code Authorization: Bearer <token>}; {@code sub} = usuário; {@code groups} = papéis ({@code cliente},
 * {@code backoffice}, {@code aprovador}, {@code tesouraria}); {@code clienteId} = cadastro do cliente (número),
 * presente quando o papel é {@code cliente}; {@code iss} = {@link #EMISSOR}.
 *
 * <p>A aplicação de teste confia numa chave pública de TESTE ({@code aceitacao/chaves/publica-teste.pem}), que
 * {@link #configuracaoDaAplicacao()} entrega ao {@link TesourariaStubResource}; os tokens são assinados com a privada
 * de teste (RS256). A assinatura usa só o JDK: nenhuma biblioteca de JWT é necessária no classpath de teste.
 */
public final class IdentidadeTeste {

    /** Emissor que a aplicação espera. */
    public static final String EMISSOR = "https://exemplo-negocial";

    public static final String CLIENTE = "cliente";
    public static final String BACKOFFICE = "backoffice";
    public static final String APROVADOR = "aprovador";
    public static final String TESOURARIA = "tesouraria";

    /** Autor das entradas da rotina de pagamento no histórico (R19). */
    public static final String SISTEMA = "sistema";

    /** Cabeçalho de um pedido sem identificação. */
    public static final String SEM_TOKEN = null;

    private static final String CHAVE_PRIVADA = "/aceitacao/chaves/privada-teste.pem";
    private static final String CHAVE_PUBLICA = "/aceitacao/chaves/publica-teste.pem";
    private static final Duration VALIDADE = Duration.ofHours(6);

    /** Quem faz o pedido: usuário (claim {@code sub}), papéis (claim {@code groups}) e, quando cliente, o cadastro. */
    public record Pessoa(String usuario, Set<String> papeis, Long clienteId) {

        public Pessoa {
            papeis = Set.copyOf(papeis);
        }

        /** Pessoa com os papéis dados e sem cadastro de cliente. */
        public static Pessoa de(String usuario, String... papeis) {
            return new Pessoa(usuario, Set.of(papeis), null);
        }

        /** O cliente do cadastro dado: papel {@code cliente} e claim {@code clienteId}. */
        public static Pessoa cliente(long clienteId) {
            return new Pessoa("cliente-" + clienteId, Set.of(CLIENTE), clienteId);
        }

        /** Valor do cabeçalho {@code Authorization} com o token válido desta pessoa. */
        public String autorizacao() {
            return bearer(token(this));
        }

        public boolean temPapel(String papel) {
            return papeis.contains(papel);
        }
    }

    // Pessoas de teste. Quem conclui a análise (backoffice) e quem aprova na alçada (aprovador) são pessoas
    // diferentes por padrão (R17); carla e dora têm os dois papéis, para provar que a regra vale por pessoa.
    public static final Pessoa ANA = Pessoa.de("ana", BACKOFFICE);
    public static final Pessoa ARI = Pessoa.de("ari", BACKOFFICE);
    public static final Pessoa BRUNO = Pessoa.de("bruno", APROVADOR);
    public static final Pessoa BIA = Pessoa.de("bia", APROVADOR);
    public static final Pessoa CARLA = Pessoa.de("carla", BACKOFFICE, APROVADOR);
    public static final Pessoa DORA = Pessoa.de("dora", BACKOFFICE, APROVADOR);
    public static final Pessoa TEREZA = Pessoa.de("tereza", TESOURARIA);
    /** Token válido, de usuário sem nenhum dos quatro papéis. */
    public static final Pessoa VISITANTE = Pessoa.de("visitante");

    private static final Map<Pessoa, String> TOKENS = new ConcurrentHashMap<>();

    private static volatile PrivateKey privada;
    private static volatile KeyPair outraChave;
    private static volatile Path publicaEmArquivo;

    private IdentidadeTeste() {
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    /** Token válido (assinatura de teste, emissor esperado, longe de expirar), guardado para não assinar a cada pedido. */
    public static String token(Pessoa pessoa) {
        return TOKENS.computeIfAbsent(pessoa, p -> {
            Instant agora = Instant.now();
            return assinar(chavePrivada(), EMISSOR, p, agora, agora.plus(VALIDADE));
        });
    }

    // ---- Tokens que a aplicação deve recusar com 401 ----

    /** Assinado por uma chave em que a aplicação não confia. */
    public static String autorizacaoAssinadaPorOutraChave(Pessoa pessoa) {
        Instant agora = Instant.now();
        return bearer(assinar(chaveDeOutroEmissor().getPrivate(), EMISSOR, pessoa, agora, agora.plus(VALIDADE)));
    }

    /** Assinatura certa, mas expirado há uma hora. */
    public static String autorizacaoExpirada(Pessoa pessoa) {
        Instant agora = Instant.now();
        return bearer(assinar(chavePrivada(), EMISSOR, pessoa, agora.minus(Duration.ofHours(2)),
                agora.minus(Duration.ofHours(1))));
    }

    /** Assinatura certa, mas emitido por quem a aplicação não espera. */
    public static String autorizacaoDeOutroEmissor(Pessoa pessoa) {
        Instant agora = Instant.now();
        return bearer(assinar(chavePrivada(), "https://outro-emissor", pessoa, agora, agora.plus(VALIDADE)));
    }

    /** Texto que não é um token. */
    public static String autorizacaoMalformada() {
        return bearer("isto-nao-e-um-token");
    }

    // ---- Configuração da aplicação sob teste ----

    /**
     * O que a aplicação precisa para confiar nos tokens de teste: a chave pública de teste (em arquivo, para valer
     * também para o artefato empacotado, que não vê o classpath de teste) e o emissor esperado.
     */
    public static Map<String, String> configuracaoDaAplicacao() {
        return Map.of("mp.jwt.verify.publickey.location", arquivoDaChavePublica().toString(),
                "mp.jwt.verify.issuer", EMISSOR);
    }

    private static synchronized Path arquivoDaChavePublica() {
        if (publicaEmArquivo == null) {
            try {
                Path arquivo = Files.createTempFile("publica-teste-", ".pem");
                Files.writeString(arquivo, lerRecurso(CHAVE_PUBLICA), StandardCharsets.UTF_8);
                arquivo.toFile().deleteOnExit();
                publicaEmArquivo = arquivo.toAbsolutePath();
            } catch (IOException e) {
                throw new IllegalStateException("Não foi possível gravar a chave pública de teste", e);
            }
        }
        return publicaEmArquivo;
    }

    /** Chave pública de teste, para quem confere assinaturas (o teste unitário dos tokens). */
    public static PublicKey chavePublica() {
        try {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(corpoDoPem(CHAVE_PUBLICA)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chave pública de teste inválida", e);
        }
    }

    /** Chave pública da chave em que a aplicação NÃO confia. */
    public static PublicKey chavePublicaDeOutroEmissor() {
        return chaveDeOutroEmissor().getPublic();
    }

    private static PrivateKey chavePrivada() {
        if (privada == null) {
            try {
                privada = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(corpoDoPem(CHAVE_PRIVADA)));
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException("Chave privada de teste inválida", e);
            }
        }
        return privada;
    }

    private static synchronized KeyPair chaveDeOutroEmissor() {
        if (outraChave == null) {
            try {
                KeyPairGenerator gerador = KeyPairGenerator.getInstance("RSA");
                gerador.initialize(2048);
                outraChave = gerador.generateKeyPair();
            } catch (GeneralSecurityException e) {
                throw new IllegalStateException(e);
            }
        }
        return outraChave;
    }

    private static byte[] corpoDoPem(String recurso) {
        String texto = lerRecurso(recurso).replaceAll("-----[A-Z ]+-----", "").replaceAll("\\s", "");
        return Base64.getDecoder().decode(texto);
    }

    private static String lerRecurso(String recurso) {
        try (InputStream entrada = IdentidadeTeste.class.getResourceAsStream(recurso)) {
            if (entrada == null) {
                throw new IllegalStateException("Recurso de teste não encontrado: " + recurso);
            }
            return new String(entrada.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível ler " + recurso, e);
        }
    }

    // ---- Assinatura (JWT, RS256) ----

    private static String assinar(PrivateKey chave, String emissor, Pessoa pessoa, Instant emitidoEm, Instant expiraEm) {
        Map<String, Object> cabecalho = new LinkedHashMap<>();
        cabecalho.put("alg", "RS256");
        cabecalho.put("typ", "JWT");

        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("iss", emissor);
        claims.put("sub", pessoa.usuario());
        claims.put("upn", pessoa.usuario());
        List<String> papeis = List.copyOf(new TreeSet<>(pessoa.papeis()));
        claims.put("groups", papeis);
        if (pessoa.clienteId() != null) {
            claims.put("clienteId", pessoa.clienteId());
        }
        claims.put("iat", emitidoEm.getEpochSecond());
        claims.put("exp", expiraEm.getEpochSecond());
        claims.put("jti", UUID.randomUUID().toString());

        String conteudo = codificar(ApiAceitacao.json(cabecalho)) + "." + codificar(ApiAceitacao.json(claims));
        try {
            Signature assinatura = Signature.getInstance("SHA256withRSA");
            assinatura.initSign(chave);
            assinatura.update(conteudo.getBytes(StandardCharsets.US_ASCII));
            return conteudo + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(assinatura.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Não foi possível assinar o token de teste", e);
        }
    }

    private static String codificar(String json) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
