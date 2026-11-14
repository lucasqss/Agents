package br.com.claudio.consorcio.aceitacao.pagamentoprocesso;

import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.VALOR_PADRAO;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.clienteDaCota;
import static br.com.claudio.consorcio.aceitacao.ApiAceitacao.novaCota;
import static br.com.claudio.consorcio.aceitacao.pagamentoprocesso.ModeloPagamentoProcesso.*;

import br.com.claudio.consorcio.aceitacao.IdentidadeTeste;
import br.com.claudio.consorcio.aceitacao.IdentidadeTeste.Pessoa;
import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Padrão: Tabela de decisão como modelo de teste [Ammann e Offutt; Myers].
 * Escrita a partir do quadro "Quem pode executar cada ação" (Atores), de R17 e de R18; nada aqui vem do código da
 * aplicação. A tabela é papel × ação, com a ação na situação em que ela vale: assim a recusa só pode ser de papel.
 *
 * <p>Quem pede é uma {@link Credencial}: o cliente dono da cota, um cliente de outra cota, cada um dos papéis de
 * pessoa da casa, um usuário válido sem nenhum papel e o pedido sem token.
 */
final class ModeloPapeis {

    /** Quem faz o pedido. Cada valor vira um token (ou a falta dele) para o processo do caso. */
    enum Credencial {
        /** Cliente da cota do processo. */
        DONO,
        /** Cliente de outra cota (papel certo, cota errada). */
        OUTRO_CLIENTE,
        BACKOFFICE,
        APROVADOR,
        TESOURARIA,
        /** Token válido de usuário sem nenhum dos quatro papéis. */
        SEM_PAPEL,
        /** Nenhum cabeçalho de identificação. */
        SEM_TOKEN;

        /**
         * Cabeçalho {@code Authorization} desta credencial para um processo da cota dada. Backoffice, aprovador e
         * tesouraria são pessoas diferentes das que os auxiliares usam por padrão ("ari", "bia", "tereza" em vez de
         * "ana", "bruno" e "carla"), para o autor do histórico não poder coincidir com o padrão por acaso.
         */
        String autorizacao(long cota) {
            Pessoa pessoa = pessoa(cota);
            return pessoa == null ? IdentidadeTeste.SEM_TOKEN : pessoa.autorizacao();
        }

        /** Quem é, para conferir o autor no histórico; nulo em {@link #SEM_TOKEN}. */
        Pessoa pessoa(long cota) {
            return switch (this) {
                case DONO -> clienteDaCota(cota);
                case OUTRO_CLIENTE -> clienteDaCota(novaCota());
                case BACKOFFICE -> IdentidadeTeste.ARI;
                case APROVADOR -> IdentidadeTeste.BIA;
                case TESOURARIA -> IdentidadeTeste.TEREZA;
                case SEM_PAPEL -> IdentidadeTeste.VISITANTE;
                case SEM_TOKEN -> null;
            };
        }

        /** Status da recusa: sem token é 401; com token válido sem o papel (ou da cota) da ação é 403. */
        int statusDaRecusa() {
            return this == SEM_TOKEN ? 401 : 403;
        }
    }

    /**
     * Uma ação de escrita sobre um processo na situação em que ela vale, com o destino e quem a executa (R18). O
     * valor do processo é o da faixa que leva à situação (aguardando alçada: acima da alçada).
     */
    record Celula(String tipo, String situacao, String acao, String destino, Set<Credencial> aceitas) {

        BigDecimal valor() {
            return AGUARDANDO_ALCADA.equals(situacao) ? VALOR_ACIMA_DA_ALCADA : VALOR_PADRAO;
        }

        boolean aceita(Credencial credencial) {
            return aceitas.contains(credencial);
        }

        @Override
        public String toString() {
            return "%s em %s (%s) -> %s".formatted(acao, situacao, tipo, destino);
        }
    }

    private static Celula celula(String tipo, String situacao, String acao, String destino, Credencial... aceitas) {
        return new Celula(tipo, situacao, acao, destino, EnumSet.copyOf(Set.of(aceitas)));
    }

    /**
     * Quadro "Quem pode executar cada ação", uma linha por ação e situação em que ela vale. Reprovar e devolver para
     * análise mudam de papel conforme a situação (backoffice, aprovador); cancelar é de dois papéis; abrir processo e
     * os cadastros têm testes à parte.
     */
    static Stream<Celula> celulas() {
        return Stream.of(
                celula(SERVICO, ABERTO, INICIAR_ANALISE, EM_ANALISE, Credencial.BACKOFFICE),
                celula(SERVICO, EM_ANALISE, SOLICITAR_DOCUMENTO, PENDENTE_DOCUMENTO, Credencial.BACKOFFICE),
                celula(SERVICO, PENDENTE_DOCUMENTO, RECEBER_DOCUMENTO, EM_ANALISE, Credencial.BACKOFFICE),
                celula(SERVICO, EM_ANALISE, CONCLUIR_ANALISE, APROVADO, Credencial.BACKOFFICE),
                celula(IMOBILIARIO, EM_ANALISE, CONCLUIR_ANALISE, AGUARDANDO_VISTORIA, Credencial.BACKOFFICE),
                celula(IMOBILIARIO, AGUARDANDO_VISTORIA, CONCLUIR_VISTORIA, APROVADO, Credencial.BACKOFFICE),
                celula(SERVICO, EM_ANALISE, REPROVAR, REPROVADO, Credencial.BACKOFFICE),
                celula(SERVICO, AGUARDANDO_ALCADA, REPROVAR, REPROVADO, Credencial.APROVADOR),
                celula(SERVICO, AGUARDANDO_ALCADA, APROVAR_NA_ALCADA, APROVADO, Credencial.APROVADOR),
                celula(SERVICO, APROVADO, DEVOLVER_PARA_ANALISE, EM_ANALISE, Credencial.BACKOFFICE),
                celula(IMOBILIARIO, AGUARDANDO_VISTORIA, DEVOLVER_PARA_ANALISE, EM_ANALISE, Credencial.BACKOFFICE),
                celula(SERVICO, AGUARDANDO_ALCADA, DEVOLVER_PARA_ANALISE, EM_ANALISE, Credencial.APROVADOR),
                celula(SERVICO, APROVADO, SOLICITAR_PAGAMENTO, PAGAMENTO_SOLICITADO, Credencial.TESOURARIA),
                celula(SERVICO, ABERTO, CANCELAR, CANCELADO, Credencial.BACKOFFICE, Credencial.APROVADOR),
                celula(SERVICO, EM_ANALISE, CANCELAR, CANCELADO, Credencial.BACKOFFICE, Credencial.APROVADOR),
                celula(SERVICO, PENDENTE_DOCUMENTO, CANCELAR, CANCELADO, Credencial.BACKOFFICE, Credencial.APROVADOR),
                celula(IMOBILIARIO, AGUARDANDO_VISTORIA, CANCELAR, CANCELADO, Credencial.BACKOFFICE,
                        Credencial.APROVADOR),
                celula(SERVICO, AGUARDANDO_ALCADA, CANCELAR, CANCELADO, Credencial.BACKOFFICE, Credencial.APROVADOR),
                celula(SERVICO, AGUARDANDO_CONCORDANCIA, CANCELAR, CANCELADO, Credencial.BACKOFFICE,
                        Credencial.APROVADOR),
                celula(SERVICO, APROVADO, CANCELAR, CANCELADO, Credencial.BACKOFFICE, Credencial.APROVADOR),
                celula(SERVICO, ABERTO, ALTERAR, ABERTO, Credencial.DONO),
                celula(SERVICO, EM_ANALISE, PROPOR_ALTERACAO, AGUARDANDO_CONCORDANCIA, Credencial.BACKOFFICE),
                celula(SERVICO, AGUARDANDO_CONCORDANCIA, CONCORDAR, EM_ANALISE, Credencial.DONO),
                celula(SERVICO, AGUARDANDO_CONCORDANCIA, DISCORDAR, EM_ANALISE, Credencial.DONO));
    }

    private ModeloPapeis() {
    }
}
