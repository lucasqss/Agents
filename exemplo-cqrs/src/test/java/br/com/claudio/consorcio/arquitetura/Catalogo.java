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
 * Catálogo corporativo: as dependências Maven geradas para consumir outros sistemas ou para prover a eles.
 * Uma classe é "do catálogo" quando casa com {@link #PACOTES} ou quando vem de um jar com o mesmo
 * {@code groupId} deste componente (detecção automática, lida do {@code pom.xml}).
 *
 * <p>ÚNICO ARQUIVO DESTA PASTA QUE SE EDITA POR PROJETO: ajuste as duas listas abaixo.
 */
final class Catalogo {

    /** Pacotes do catálogo, um por sistema. Ex.: {@code "org.acme.tesouraria.."} (com os subpacotes). */
    static final List<String> PACOTES = List.of(
            "org.acme.sistema..");

    /** Pacotes de jars com o mesmo groupId que NÃO são do catálogo (biblioteca interna da equipe, por exemplo). */
    static final List<String> FORA_DO_CATALOGO = List.of();

    private static final Optional<String> GROUP_ID = lerGroupId();

    private static final Map<String, Boolean> JAR_DO_GROUP_ID = new ConcurrentHashMap<>();

    private Catalogo() {
    }

    /** Classe gerada pelo catálogo corporativo. */
    static DescribedPredicate<JavaClass> classeDoCatalogo() {
        return DescribedPredicate.describe("são do catálogo corporativo", Catalogo::ehDoCatalogo);
    }

    static boolean ehDoCatalogo(JavaClass classe) {
        String pacote = classe.getPackageName();
        if (casa(FORA_DO_CATALOGO, pacote) || pacote.startsWith(Raiz.PACOTE + ".") || pacote.equals(Raiz.PACOTE)) {
            return false;
        }
        return casa(PACOTES, pacote) || vemDeJarDoMesmoGroupId(classe);
    }

    private static boolean casa(List<String> padroes, String pacote) {
        return padroes.stream().anyMatch(padrao -> PackageMatcher.of(padrao).matches(pacote));
    }

    /** O jar traz {@code META-INF/maven/<groupId>/...} (padrão do Maven) ou está na pasta do groupId no repositório local. */
    private static boolean vemDeJarDoMesmoGroupId(JavaClass classe) {
        if (GROUP_ID.isEmpty() || classe.getSource().isEmpty()) {
            return false;
        }
        String uri = classe.getSource().get().getUri().toString();
        int fim = uri.indexOf("!/");
        if (!uri.startsWith("jar:file:") || fim < 0) {
            return false;
        }
        return JAR_DO_GROUP_ID.computeIfAbsent(uri.substring("jar:".length(), fim), Catalogo::jarDoGroupId);
    }

    private static boolean jarDoGroupId(String uriDoJar) {
        String groupId = GROUP_ID.orElseThrow();
        if (uriDoJar.contains("/" + groupId.replace('.', '/') + "/")) {
            return true;
        }
        try (JarFile jar = new JarFile(new File(URI.create(uriDoJar)))) {
            String descritor = "META-INF/maven/" + groupId + "/";
            return jar.stream().anyMatch(entrada -> entrada.getName().startsWith(descritor));
        } catch (Exception e) {
            return false;
        }
    }

    /** {@code <groupId>} do pom.xml do componente; se não houver, o do {@code <parent>}. */
    private static Optional<String> lerGroupId() {
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
