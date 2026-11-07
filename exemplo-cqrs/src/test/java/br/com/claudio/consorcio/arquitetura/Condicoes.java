package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

/** Predicados e condições customizados usados pelas regras. */
final class Condicoes {

    private Condicoes() {
    }

    /**
     * Segmento de pacote logo depois do marcador, ou {@code null}. Ex.: para
     * "x.query.entrypoint.mobile.foo" e marcador "entrypoint", devolve "mobile".
     */
    static String segmentoApos(String pacote, String marcador) {
        String[] partes = pacote.split("\\.");
        String[] alvo = marcador.split("\\.");
        for (int i = 0; i + alvo.length < partes.length; i++) {
            boolean casa = true;
            for (int j = 0; j < alvo.length; j++) {
                casa &= partes[i + j].equals(alvo[j]);
            }
            if (casa) {
                return partes[i + alvo.length];
            }
        }
        return null;
    }

    /** Client e DTOs externos: tudo em shared.gateway que não seja *Gateway nem *Exception. */
    static DescribedPredicate<JavaClass> internoDoGateway() {
        return DescribedPredicate.describe("são client ou DTO de sistema externo",
                c -> PackageMatcher.of(Raiz.pacote("shared.gateway")).matches(c.getPackageName())
                        && !c.getSimpleName().endsWith("Gateway")
                        && !c.getSimpleName().endsWith("Exception"));
    }

    /** Classe que implementa uma interface declarada em command.domain. */
    static DescribedPredicate<JavaClass> implementaInterfaceDoDominio() {
        return DescribedPredicate.describe("implementam uma interface do domínio",
                c -> c.getAllRawInterfaces().stream()
                        .anyMatch(i -> PackageMatcher.of(Raiz.pacote("command.domain")).matches(i.getPackageName())));
    }

    /** Classe que depende de algo em shared.gateway. */
    static DescribedPredicate<JavaClass> usamGateway() {
        return DescribedPredicate.describe("usam shared.gateway",
                c -> c.getDirectDependenciesFromSelf().stream()
                        .anyMatch(d -> PackageMatcher.of(Raiz.pacote("shared.gateway"))
                                .matches(d.getTargetClass().getPackageName())));
    }

    /**
     * O padrão de apresentação do canal X (shared.apresentacao.X) só é usado pelo entrypoint do mesmo canal
     * (..entrypoint.X..) ou pelo próprio pacote. Sem lista de canais: um canal novo herda a regra.
     */
    static ArchCondition<JavaClass> usadoSoPeloProprioCanal() {
        return new ArchCondition<>("ser usado só pelo entrypoint do próprio canal") {
            @Override
            public void check(JavaClass alvo, ConditionEvents eventos) {
                String canal = segmentoApos(alvo.getPackageName(), "shared.apresentacao");
                for (Dependency dependencia : alvo.getDirectDependenciesToSelf()) {
                    String origem = dependencia.getOriginClass().getPackageName();
                    boolean permitido = canal.equals(segmentoApos(origem, "entrypoint"))
                            || canal.equals(segmentoApos(origem, "shared.apresentacao"));
                    if (!permitido) {
                        eventos.add(SimpleConditionEvent.violated(dependencia,
                                "padrão do canal '" + canal + "' usado fora dele: " + dependencia.getDescription()));
                    }
                }
            }
        };
    }

    /** Método público não pode devolver um tipo de nenhum pacote persistence (o contrato do canal é próprio). */
    static ArchCondition<JavaMethod> naoRetornarTipoDePersistence() {
        return new ArchCondition<>("não retornar tipo de ..persistence..") {
            @Override
            public void check(JavaMethod metodo, ConditionEvents eventos) {
                String pacote = metodo.getRawReturnType().getPackageName();
                if (PackageMatcher.of(Raiz.emQualquer("persistence")).matches(pacote)) {
                    eventos.add(SimpleConditionEvent.violated(metodo,
                            metodo.getFullName() + " retorna " + metodo.getRawReturnType().getName()));
                }
            }
        };
    }
}
