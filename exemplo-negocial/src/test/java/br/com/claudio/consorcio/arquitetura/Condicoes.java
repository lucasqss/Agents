package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Predicados e condições customizados usados pelas regras. */
final class Condicoes {

    /** Vias de entrada que não são canal de atendimento. */
    private static final Set<String> VIAS_QUE_NAO_SAO_CANAL = Set.of("rest", "mensageria", "batch", "apresentacao");

    /** Anotações pelo nome: o componente pode não ter a dependência (ACL e BFF não têm JPA, por exemplo). */
    static final String ENTITY = "jakarta.persistence.Entity";
    static final String REPOSITORY = "jakarta.data.repository.Repository";
    static final String TRANSACTIONAL = "jakarta.transaction.Transactional";
    static final String REST_CLIENT = "org.eclipse.microprofile.rest.client.inject.RegisterRestClient";

    private Condicoes() {
    }

    /**
     * Segmento de pacote logo depois do marcador, ou {@code null}. Ex.: para
     * "x.entrypoint.mobile.cota" e marcador "entrypoint", devolve "mobile".
     */
    static String segmentoApos(String pacote, String marcador) {
        String[] partes = pacote.split("\\.");
        for (int i = 0; i + 1 < partes.length; i++) {
            if (partes[i].equals(marcador)) {
                return partes[i + 1];
            }
        }
        return null;
    }

    private static boolean estaEm(JavaClass classe, String pacote) {
        return PackageMatcher.of(pacote).matches(classe.getPackageName());
    }

    /** Classe de {@code entrypoint.<canal>}: tudo em entrypoint que não seja rest, mensageria, batch ou apresentacao. */
    static DescribedPredicate<JavaClass> estaoEmCanal() {
        return DescribedPredicate.describe("estão em um canal",
                c -> {
                    String via = estaEm(c, Raiz.pacote("entrypoint")) ? segmentoApos(c.getPackageName(), "entrypoint") : null;
                    return via != null && !VIAS_QUE_NAO_SAO_CANAL.contains(via);
                });
    }

    /**
     * O que não sai do gateway: o client e o modelo do sistema externo. Modelo externo é a classe que usa
     * {@link ClassesExternas}, que aparece no *Client (direta ou indiretamente) ou que depende de uma dessas.
     * O que sobra (*Gateway, *Exception e os records próprios, no nosso vocabulário) pode ser usado pelo use case.
     */
    static DescribedPredicate<JavaClass> internoDoGateway() {
        return DescribedPredicate.describe("são client ou modelo de sistema externo",
                c -> estaEm(c, Raiz.pacote("gateway"))
                        && !fachadaDoGateway(c)
                        && (modeloExterno(c, new HashSet<>())
                                || c.getDirectDependenciesFromSelf().stream()
                                        .map(Dependency::getTargetClass)
                                        .anyMatch(alvo -> modeloExterno(alvo, new HashSet<>()))));
    }

    private static boolean fachadaDoGateway(JavaClass c) {
        return c.getSimpleName().endsWith("Gateway")
                || c.getSimpleName().endsWith("GatewayAdapter")
                || c.getSimpleName().endsWith("Exception");
    }

    private static boolean modeloExterno(JavaClass c, Set<JavaClass> visitadas) {
        if (!estaEm(c, Raiz.pacote("gateway")) || fachadaDoGateway(c) || !visitadas.add(c)) {
            return false;
        }
        if (c.getSimpleName().endsWith("Client") || c.isAnnotatedWith(REST_CLIENT)) {
            return true;
        }
        if (c.getDirectDependenciesFromSelf().stream().anyMatch(d -> ClassesExternas.externa(d.getTargetClass()))) {
            return true;
        }
        return c.getDirectDependenciesToSelf().stream()
                .map(Dependency::getOriginClass)
                .anyMatch(origem -> modeloExterno(origem, visitadas));
    }

    /** Classe que implementa uma interface declarada em domain. */
    static DescribedPredicate<JavaClass> implementaInterfaceDoDominio() {
        return DescribedPredicate.describe("implementam uma interface do domínio",
                c -> c.getAllRawInterfaces().stream().anyMatch(i -> estaEm(i, Raiz.pacote("domain"))));
    }

    /** Classe que depende de algo em gateway. */
    static DescribedPredicate<JavaClass> usamGateway() {
        return DescribedPredicate.describe("usam gateway",
                c -> c.getDirectDependenciesFromSelf().stream().anyMatch(d -> estaEm(d.getTargetClass(), Raiz.pacote("gateway"))));
    }

    /** Classe que depende de algo em domain: é assim que um caso de uso de escrita se identifica. */
    static DescribedPredicate<JavaClass> usamDominio() {
        return DescribedPredicate.describe("usam o domínio",
                c -> c.getDirectDependenciesFromSelf().stream().anyMatch(d -> estaEm(d.getTargetClass(), Raiz.pacote("domain"))));
    }

    /** Implementação da escrita: só o adapter a conhece. */
    static DescribedPredicate<JavaClass> implementacaoDaEscrita() {
        return DescribedPredicate.describe("são DataRepository, Mapper ou RepositoryAdapter",
                c -> estaEm(c, Raiz.pacote("persistence"))
                        && (c.getSimpleName().endsWith("DataRepository")
                                || c.getSimpleName().endsWith("Mapper")
                                || c.getSimpleName().endsWith("RepositoryAdapter")));
    }

    /** O caso de uso faz uma coisa só: exatamente um método público declarado. */
    static ArchCondition<JavaClass> terUmUnicoMetodoPublico() {
        return new ArchCondition<>("ter um único método público") {
            @Override
            public void check(JavaClass classe, ConditionEvents eventos) {
                List<String> publicos = classe.getMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(m -> !m.getModifiers().contains(JavaModifier.SYNTHETIC))
                        .map(JavaMethod::getName)
                        .sorted()
                        .toList();
                if (publicos.size() != 1) {
                    eventos.add(SimpleConditionEvent.violated(classe,
                            classe.getName() + " tem " + publicos.size() + " métodos públicos: " + publicos));
                }
            }
        };
    }

    /** Método público não pode devolver um tipo da camada dada (o contrato do canal é próprio). */
    static ArchCondition<JavaMethod> naoRetornarTipoDe(String camada) {
        return new ArchCondition<>("não retornar tipo de " + camada) {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                if (estaEm(metodo.getRawReturnType(), Raiz.pacote(camada))) {
                    eventos.add(SimpleConditionEvent.violated(metodo,
                            metodo.getFullName() + " retorna " + metodo.getRawReturnType().getName()));
                }
            }
        };
    }

    /**
     * Onde o modelo de um sistema externo pode aparecer: no gateway e, em ACL (o tipo que tem translator),
     * também no translator e no use case, que leva o modelo do Gateway ao Translator.
     */
    static String[] ondeOModeloExternoPode() {
        if (Tipo.CAMADAS.contains("translator")) {
            return new String[] {Raiz.pacote("gateway"), Raiz.pacote("translator"), Raiz.pacote("application")};
        }
        return new String[] {Raiz.pacote("gateway")};
    }

    /** Método público não pode devolver client, DTO externo nem classe de dependência de outro sistema. */
    static ArchCondition<JavaMethod> naoRetornarModeloExterno() {
        DescribedPredicate<JavaClass> interno = internoDoGateway();
        return new ArchCondition<>("não retornar modelo de sistema externo") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                JavaClass retorno = metodo.getRawReturnType();
                if (interno.test(retorno) || ClassesExternas.externa(retorno)) {
                    eventos.add(SimpleConditionEvent.violated(metodo,
                            metodo.getFullName() + " retorna " + retorno.getName()));
                }
            }
        };
    }

    /** O componente atende um canal só: no máximo um pacote de canal em entrypoint. */
    static ArchCondition<JavaClass> estarNoUnicoCanal() {
        return new ArchCondition<>("estar no único canal do componente") {
            private String primeiro;

            @Override
            public void check(JavaClass classe, ConditionEvents eventos) {
                String canal = segmentoApos(classe.getPackageName(), "entrypoint");
                if (primeiro == null) {
                    primeiro = canal;
                } else if (!primeiro.equals(canal)) {
                    eventos.add(SimpleConditionEvent.violated(classe,
                            classe.getName() + " está no canal " + canal + ", mas o componente já atende " + primeiro));
                }
            }
        };
    }
}
