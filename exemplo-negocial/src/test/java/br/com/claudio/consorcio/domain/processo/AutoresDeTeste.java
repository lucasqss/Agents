package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import java.util.Set;

/**
 * Pessoas dos testes do domínio e, para cada ação, quem a pode pedir em qualquer situação em que ela vale. Quem conclui
 * a análise (ana) e quem aprova na alçada (bruno) são pessoas diferentes por padrão (R17); carla tem os papéis de
 * backoffice e de aprovador, porque reprovar e devolver para análise mudam de papel conforme a situação.
 */
public final class AutoresDeTeste {

    public static final Autor ANA = new Autor("ana", Set.of(Papel.BACKOFFICE), null);
    public static final Autor ARI = new Autor("ari", Set.of(Papel.BACKOFFICE), null);
    public static final Autor BRUNO = new Autor("bruno", Set.of(Papel.APROVADOR), null);
    public static final Autor BIA = new Autor("bia", Set.of(Papel.APROVADOR), null);
    public static final Autor CARLA = new Autor("carla", Set.of(Papel.BACKOFFICE, Papel.APROVADOR), null);
    public static final Autor TEREZA = new Autor("tereza", Set.of(Papel.TESOURARIA), null);
    /** O cliente do cadastro 1, o das cotas dos testes. */
    public static final Autor CLIENTE = new Autor("cliente-1", Set.of(Papel.CLIENTE), 1L);

    private AutoresDeTeste() {
    }

    /** Quem pede a ação nos testes que não são sobre permissão. */
    public static Autor quemFaz(AcaoProcesso acao) {
        return switch (acao) {
            case APROVAR_NA_ALCADA -> BRUNO;
            case REPROVAR, DEVOLVER_PARA_ANALISE -> CARLA;
            case SOLICITAR_PAGAMENTO -> TEREZA;
            case ALTERAR, CONCORDAR, DISCORDAR -> CLIENTE;
            default -> ANA;
        };
    }

    /** Autor esperado no histórico de uma ação pedida por {@link #quemFaz}: o usuário, ou "sistema" nas da rotina. */
    public static String usuarioDe(AcaoProcesso acao) {
        return acao.daRotina() && acao != AcaoProcesso.SOLICITAR_PAGAMENTO ? Transicao.AUTOR_SISTEMA
                : quemFaz(acao).usuario();
    }
}
