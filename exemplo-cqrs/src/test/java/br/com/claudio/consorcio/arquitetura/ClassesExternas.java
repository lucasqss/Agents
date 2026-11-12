package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.PackageMatcher;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarFile;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Classes externas: as que chegam em dependências Maven de outros sistemas (catálogo corporativo), para
 * consumi-los ou para prover a eles. Uma classe é externa quando vem de um jar cujo {@code groupId} está
 * na lista de {@link #CONFIGURACAO}. Elas só podem aparecer em gateway e entrypoint (TEC-011, SHR-003).
 *
 * <p>ÚNICO ARQUIVO DESTA PASTA QUE SE EDITA POR PROJETO: acrescente um groupId por sistema externo.
 */
final class ClassesExternas {

    /** Marcador: o {@code groupId} deste componente, lido do pom.xml (ou do {@code <parent>} dele). */
    static final String GROUP_ID_DO_PROJETO = "${project.groupId}";

    static final ClassesExternas CONFIGURACAO = new ClassesExternas(
            GROUP_ID_DO_PROJETO,
            "org.acme.sistema");

    /** Pacotes de jars desses groupIds que NÃO são de sistema externo (biblioteca interna da equipe, por exemplo). */
    private static final List<String> PACOTES_QUE_NAO_SAO_EXTERNOS = List.of();

    private final List<String> groupIds;

    private final Map<String, Boolean> jarExterno = new ConcurrentHashMap<>();

    /** Um groupId por sistema externo, exato (subgrupos não entram sozinhos). */
    private ClassesExternas(String... groupIds) {
        this.groupIds = List.of(groupIds).stream()
                .map(groupId -> GROUP_ID_DO_PROJETO.equals(groupId) ? lerGroupIdDoPom().orElse(null) : groupId)
                .filter(groupId -> groupId != null && !groupId.isBlank())
                .toList();
    }

    /** Classe que vem de dependência de outro sistema. */
    static DescribedPredicate<JavaClass> externas() {
        return DescribedPredicate.describe("são de dependência de outro sistema", CONFIGURACAO::ehExterna);
    }

    static boolean externa(JavaClass classe) {
        return CONFIGURACAO.ehExterna(classe);
    }

    private boolean ehExterna(JavaClass classe) {
        String pacote = classe.getPackageName();
        if (pacote.equals(Raiz.PACOTE) || pacote.startsWith(Raiz.PACOTE + ".")
                || PACOTES_QUE_NAO_SAO_EXTERNOS.stream().anyMatch(padrao -> PackageMatcher.of(padrao).matches(pacote))) {
            return false;
        }
        if (classe.getSource().isEmpty()) {
            return false;
        }
        String uri = classe.getSource().get().getUri().toString();
        int fim = uri.indexOf("!/");
        if (!uri.startsWith("jar:file:") || fim < 0) {
            return false;
        }
        return jarExterno.computeIfAbsent(uri.substring("jar:".length(), fim), this::jarDeGroupIdListado);
    }

    /**
     * Decide pelo descritor que o Maven grava no jar ({@code META-INF/maven/<groupId>/<artifactId>/}). Jar sem
     * descritor: decide pela pasta do repositório local ({@code <groupId>/<artifactId>/<versão>/arquivo.jar}).
     */
    private boolean jarDeGroupIdListado(String uriDoJar) {
        File arquivo = new File(URI.create(uriDoJar));
        try (JarFile jar = new JarFile(arquivo)) {
            List<String> descritores = jar.stream()
                    .map(entrada -> entrada.getName())
                    .filter(nome -> nome.startsWith("META-INF/maven/") && nome.endsWith("/pom.properties"))
                    .toList();
            if (!descritores.isEmpty()) {
                return descritores.stream().anyMatch(nome -> groupIds.contains(nome.split("/")[2]));
            }
        } catch (Exception e) {
            return false;
        }
        File pastaDoGroupId = Optional.ofNullable(arquivo.getParentFile())
                .map(File::getParentFile).map(File::getParentFile).orElse(null);
        if (pastaDoGroupId == null) {
            return false;
        }
        String caminho = pastaDoGroupId.toURI().getPath();
        return groupIds.stream().anyMatch(groupId -> caminho.endsWith("/" + groupId.replace('.', '/') + "/"));
    }

    private static Optional<String> lerGroupIdDoPom() {
        Path pom = Path.of("pom.xml");
        if (!Files.isRegularFile(pom)) {
            return Optional.empty();
        }
        try {
            Element projeto = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                    .parse(pom.toFile()).getDocumentElement();
            Optional<String> proprio = filho(projeto, "groupId").map(Element::getTextContent);
            if (proprio.isPresent()) {
                return proprio.map(String::strip);
            }
            return filho(projeto, "parent").flatMap(pai -> filho(pai, "groupId"))
                    .map(Element::getTextContent).map(String::strip);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Optional<Element> filho(Element pai, String nome) {
        for (Node no = pai.getFirstChild(); no != null; no = no.getNextSibling()) {
            if (no instanceof Element elemento && nome.equals(elemento.getTagName())) {
                return Optional.of(elemento);
            }
        }
        return Optional.empty();
    }
}
