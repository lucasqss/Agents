package br.com.claudio.consorcio.arquitetura;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaPackage;
import com.tngtech.archunit.core.domain.PackageMatcher;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Optional;

/** Predicados e condições customizados usados pelas regras. */
final class Condicoes {

    /** Anotação que marca o pacote raiz de um canal com escrita bloqueada (no package-info do canal). */
    private static final String SOMENTE_LEITURA = Raiz.exato("canal.comum.SomenteLeitura");

    private Condicoes() {
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
     * Classe de um canal com escrita bloqueada: o próprio pacote ou um pacote acima (até canal.&lt;canal&gt;) tem
     * {@code @SomenteLeitura} no package-info. Sem lista de canais: marcar o package-info basta.
     */
    static DescribedPredicate<JavaClass> deCanalSomenteLeitura() {
        return DescribedPredicate.describe("são de canal @SomenteLeitura", c -> {
            Optional<JavaPackage> pacote = Optional.of(c.getPackage());
            while (pacote.isPresent()) {
                if (pacote.get().isAnnotatedWith(SOMENTE_LEITURA)) {
                    return true;
                }
                pacote = pacote.get().getParent();
            }
            return false;
        });
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
