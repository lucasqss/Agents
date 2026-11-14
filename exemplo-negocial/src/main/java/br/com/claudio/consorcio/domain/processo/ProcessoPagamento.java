package br.com.claudio.consorcio.domain.processo;

import br.com.claudio.consorcio.domain.Dinheiro;
import br.com.claudio.consorcio.domain.DominioException;
import br.com.claudio.consorcio.domain.PermissaoNegadaException;
import br.com.claudio.consorcio.domain.processo.fluxo.AcaoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.ContextoDaTransicao;
import br.com.claudio.consorcio.domain.processo.fluxo.EstadoProcesso;
import br.com.claudio.consorcio.domain.processo.fluxo.Papel;
import br.com.claudio.consorcio.domain.processo.fluxo.Transicao;
import br.com.claudio.consorcio.domain.processo.fluxo.TransicaoInvalidaException;
import br.com.claudio.consorcio.domain.processo.tipo.TipoProcesso;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Motor único dos processos de pagamento, de todos os tipos. Ele não conhece nenhum tipo em particular:
 * pergunta ao fluxo do tipo (tabela de transições) se a ação vale no estado atual e para onde ela leva.
 * Padrão: Factory Method em novo/reconstituir [GoF].
 */
public final class ProcessoPagamento {

    private final Long id;
    private final Long cotaId;
    private final TipoProcesso tipo;
    private Dinheiro valor;
    private List<Favorecido> favorecidos;
    private EstadoProcesso estado;
    private String protocoloPagamento;
    private AlteracaoProposta proposta;
    private IdentificacaoSolicitacao identificacaoSolicitacao;
    private String autorDaUltimaConclusaoDaAnalise;
    private final long versao;
    private final List<Transicao> novasTransicoes = new ArrayList<>();

    private ProcessoPagamento(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor,
            List<Favorecido> favorecidos, EstadoProcesso estado, String protocoloPagamento,
            AlteracaoProposta proposta, IdentificacaoSolicitacao identificacaoSolicitacao,
            String autorDaUltimaConclusaoDaAnalise, long versao) {
        if (cotaId == null) {
            throw new DominioException("Processo precisa de uma cota");
        }
        if (tipo == null) {
            throw new DominioException("Tipo do processo é obrigatório");
        }
        if (estado == null) {
            throw new DominioException("Estado do processo é obrigatório");
        }
        exigirDivisaoValida(valor, favorecidos);
        this.id = id;
        this.cotaId = cotaId;
        this.tipo = tipo;
        this.valor = valor;
        this.favorecidos = List.copyOf(favorecidos);
        if ((estado == EstadoProcesso.PAGO) != (protocoloPagamento != null)) {
            throw new DominioException("Processo pago tem protocolo de pagamento, e só ele");
        }
        if ((estado == EstadoProcesso.AGUARDANDO_CONCORDANCIA) != (proposta != null)) {
            throw new DominioException("Processo tem alteração proposta se, e somente se, aguarda a concordância");
        }
        if ((estado == EstadoProcesso.PAGAMENTO_SOLICITADO) != (identificacaoSolicitacao != null)) {
            throw new DominioException(
                    "Processo tem identificação da solicitação de pagamento se, e somente se, está com o pagamento solicitado");
        }
        this.estado = estado;
        this.protocoloPagamento = protocoloPagamento;
        this.proposta = proposta;
        this.identificacaoSolicitacao = identificacaoSolicitacao;
        this.autorDaUltimaConclusaoDaAnalise = autorDaUltimaConclusaoDaAnalise;
        this.versao = versao;
    }

    /**
     * Cria um processo novo, no estado Aberto, do tipo e do valor informados (R12: maior que zero, até duas casas),
     * dividido entre os favorecidos (R15, R29): ao menos um, partes maiores que zero somando o valor, sem documento repetido.
     */
    public static ProcessoPagamento novo(TipoProcesso tipo, Long cotaId, Dinheiro valor, List<Favorecido> favorecidos) {
        return new ProcessoPagamento(null, cotaId, tipo, valor, favorecidos, EstadoProcesso.ABERTO, null, null, null, null, 0);
    }

    /**
     * Recria um processo já gravado, no estado em que ele estava; a alteração proposta existe só em Aguardando
     * concordância, e a identificação da solicitação só em Pagamento solicitado. {@code autorDaUltimaConclusaoDaAnalise}
     * é o usuário que concluiu a análise pela última vez (R17); nulo se nunca concluiu (ou o processo é anterior a R17).
     */
    public static ProcessoPagamento reconstituir(Long id, Long cotaId, TipoProcesso tipo, Dinheiro valor,
            List<Favorecido> favorecidos, EstadoProcesso estado, String protocoloPagamento,
            AlteracaoProposta proposta, IdentificacaoSolicitacao identificacaoSolicitacao,
            String autorDaUltimaConclusaoDaAnalise, long versao) {
        return new ProcessoPagamento(id, cotaId, tipo, valor, favorecidos, estado, protocoloPagamento, proposta,
                identificacaoSolicitacao, autorDaUltimaConclusaoDaAnalise, versao);
    }

    /**
     * Aplica a ação; se o fluxo do tipo não a aceita no estado atual, lança TransicaoInvalidaException.
     * Ação que exige motivo ({@link AcaoProcesso#exigeMotivo()}) é recusada sem ele; o motivo vai ao histórico
     * como observação. Ação com método próprio ({@link AcaoProcesso#comMetodoProprio()}) é recusada aqui.
     * Concordar torna vigente a alteração proposta; discordar e cancelar a descartam.
     * Quem pede ({@code autor}) é conferido contra a linha da tabela do fluxo: o papel (R18) e, em Aguardando alçada, não
     * ser quem concluiu a análise (R17); recusa com {@link PermissaoNegadaException}, sem mudar nada. A ordem das recusas
     * é: a ação vale no estado (409), a pessoa pode (403), o dado está certo (400).
     * A alçada em vigor (R16) é entregue por quem chama, como {@link Dinheiro}: o domínio não lê configuração. Quando a
     * linha da tabela é condicional (concluir a análise ou a vistoria), a tabela decide o destino com o valor vigente e a
     * alçada; aqui não há comparação de valor.
     */
    public void executar(Autor autor, AcaoProcesso acao, Instant quando, String observacao, Dinheiro alcada) {
        tipo.fluxo().proximo(estado, acao, contexto(alcada));
        if (acao.comMetodoProprio()) {
            throw new DominioException("Para " + acao.descricao() + " um processo é preciso informar os dados da ação");
        }
        exigirPermissao(autor, acao);
        if (acao.exigeMotivo() && (observacao == null || observacao.isBlank())) {
            throw new DominioException("Motivo é obrigatório para " + acao.descricao() + " um processo");
        }
        transitar(autor.usuario(), acao, quando, observacao, null, null, alcada);
        if (acao == AcaoProcesso.CONCLUIR_ANALISE) {
            autorDaUltimaConclusaoDaAnalise = autor.usuario();
        }
        if (acao == AcaoProcesso.CONCORDAR) {
            valor = proposta.valor();
            favorecidos = proposta.favorecidos();
        }
        if (estado != EstadoProcesso.AGUARDANDO_CONCORDANCIA) {
            proposta = null;
        }
    }

    /**
     * Troca o valor e os favorecidos vigentes (R24, só em Aberto), com as mesmas regras da abertura (R12, R15, R29).
     * Recusa o dado inválido antes de olhar o estado, e nada muda quando recusa.
     */
    public void alterar(Autor autor, Dinheiro novoValor, List<Favorecido> novosFavorecidos, Instant quando) {
        exigirDivisaoValida(novoValor, novosFavorecidos);
        exigirPermissao(autor, AcaoProcesso.ALTERAR);
        transitar(autor.usuario(), AcaoProcesso.ALTERAR, quando, null, valor, novoValor);
        valor = novoValor;
        favorecidos = List.copyOf(novosFavorecidos);
    }

    /**
     * Guarda a alteração pedida pelo backoffice (R25, só em Análise) e passa a aguardar o cliente. O valor e os
     * favorecidos vigentes continuam os mesmos até {@code CONCORDAR}. O motivo é obrigatório e vai ao histórico.
     */
    public void proporAlteracao(Autor autor, Dinheiro novoValor, List<Favorecido> novosFavorecidos, String motivo,
            Instant quando) {
        AlteracaoProposta nova = new AlteracaoProposta(novoValor, novosFavorecidos, motivo);
        exigirPermissao(autor, AcaoProcesso.PROPOR_ALTERACAO);
        transitar(autor.usuario(), AcaoProcesso.PROPOR_ALTERACAO, quando, nova.motivo(), valor, novoValor);
        proposta = nova;
    }

    /**
     * Solicita o pagamento (R7): só leva o processo a Pagamento solicitado e guarda a identificação desta solicitação,
     * que a rotina repete em todas as tentativas (R22). Nada é enviado à tesouraria aqui.
     */
    public void solicitarPagamento(Autor autor, IdentificacaoSolicitacao identificacao, Instant quando,
            String observacao) {
        if (identificacao == null) {
            throw new DominioException("Identificação da solicitação de pagamento é obrigatória");
        }
        exigirPermissao(autor, AcaoProcesso.SOLICITAR_PAGAMENTO);
        transitar(autor.usuario(), AcaoProcesso.SOLICITAR_PAGAMENTO, quando, observacao, null, null);
        identificacaoSolicitacao = identificacao;
    }

    /**
     * Aplica o resultado da tesouraria, só se o processo ainda espera o pagamento daquela solicitação: outra execução da
     * rotina pode ter aplicado o resultado antes (nada muda, devolve falso). Confirmado leva a Pago (R20); recusado volta a
     * Aprovado com o motivo (R21); sem resposta ou incoerente continua em Pagamento solicitado e a tentativa vai ao
     * histórico (R22, R31).
     */
    public boolean aplicarResultado(IdentificacaoSolicitacao identificacao, ResultadoPagamento resultado, Instant quando) {
        if (!aguardaResultadoDe(identificacao)) {
            return false;
        }
        switch (resultado) {
            case ResultadoPagamento.Confirmado confirmado -> confirmarPagamento(confirmado.protocolo(), quando);
            case ResultadoPagamento.Recusado recusado -> recusarPagamento(recusado.motivo(), quando);
            case ResultadoPagamento.SemResposta semResposta ->
                    registrarTentativaSemResultado(semResposta.descricao(), quando);
            case ResultadoPagamento.Incoerente incoerente ->
                    registrarTentativaSemResultado(incoerente.descricao(), quando);
        }
        return true;
    }

    /** O processo está em Pagamento solicitado, esperando a rotina, e é desta solicitação. */
    public boolean aguardaResultadoDe(IdentificacaoSolicitacao identificacao) {
        return estado == EstadoProcesso.PAGAMENTO_SOLICITADO && identificacaoSolicitacao.equals(identificacao);
    }

    /** A tesouraria confirmou: paga o processo e guarda o protocolo, numa operação só, e descarta a identificação. */
    public void confirmarPagamento(String protocolo, Instant quando) {
        if (protocolo == null || protocolo.isBlank()) {
            throw new DominioException("Protocolo de pagamento é obrigatório");
        }
        transitar(Transicao.AUTOR_SISTEMA, AcaoProcesso.CONFIRMAR_PAGAMENTO, quando, "Protocolo " + protocolo.strip(), null, null);
        protocoloPagamento = protocolo.strip();
        identificacaoSolicitacao = null;
    }

    /** A tesouraria recusou: volta a Aprovado, com o motivo no histórico; a identificação é descartada (R21). */
    public void recusarPagamento(String motivo, Instant quando) {
        exigirTexto(AcaoProcesso.RECUSAR_PAGAMENTO, motivo);
        transitar(Transicao.AUTOR_SISTEMA, AcaoProcesso.RECUSAR_PAGAMENTO, quando, limitar(motivo), null, null);
        identificacaoSolicitacao = null;
    }

    /** Tentativa sem resultado aplicado (sem resposta ou resposta incoerente): continua esperando, com o fato no histórico. */
    public void registrarTentativaSemResultado(String observacao, Instant quando) {
        exigirTexto(AcaoProcesso.TENTAR_PAGAMENTO, observacao);
        transitar(Transicao.AUTOR_SISTEMA, AcaoProcesso.TENTAR_PAGAMENTO, quando, limitar(observacao), null, null);
    }

    private static void exigirTexto(AcaoProcesso acao, String texto) {
        if (acao.exigeMotivo() && (texto == null || texto.isBlank())) {
            throw new DominioException("Motivo é obrigatório para " + acao.descricao() + " um processo");
        }
    }

    /** O que a tesouraria diz pode passar do tamanho do histórico: guarda o começo, sem recusar o resultado. */
    private static String limitar(String texto) {
        String limpo = texto.strip();
        return limpo.length() <= Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO
                ? limpo
                : limpo.substring(0, Transicao.TAMANHO_MAXIMO_DA_OBSERVACAO - 3) + "...";
    }

    /** Valor do processo e favorecidos: valor maior que zero e divisão exata entre os favorecidos (R12, R15, R29). */
    static void exigirDivisaoValida(Dinheiro valor, List<Favorecido> favorecidos) {
        if (valor == null || !valor.positivo()) {
            throw new DominioException("Valor do processo precisa ser maior que zero");
        }
        if (favorecidos == null || favorecidos.isEmpty()) {
            throw new DominioException("Processo precisa de ao menos um favorecido");
        }
        Set<Documento> documentos = new HashSet<>();
        Dinheiro soma = Dinheiro.ZERO;
        for (Favorecido favorecido : favorecidos) {
            if (favorecido == null) {
                throw new DominioException("Favorecido não pode ser nulo");
            }
            if (!documentos.add(favorecido.documento())) {
                throw new DominioException("Favorecido repetido: o documento " + favorecido.documento().numero()
                        + " aparece mais de uma vez");
            }
            soma = soma.somar(favorecido.parte());
        }
        if (soma.compareTo(valor) < 0) {
            throw new DominioException("As partes dos favorecidos somam " + soma.valor().toPlainString() + " e faltam "
                    + valor.subtrair(soma).valor().toPlainString() + " para o valor do processo ("
                    + valor.valor().toPlainString() + ")");
        }
        if (soma.compareTo(valor) > 0) {
            throw new DominioException("As partes dos favorecidos somam " + soma.valor().toPlainString() + " e passam "
                    + soma.subtrair(valor).valor().toPlainString() + " do valor do processo ("
                    + valor.valor().toPlainString() + ")");
        }
    }

    private void transitar(String autor, AcaoProcesso acao, Instant quando, String observacao, Dinheiro valorAnterior,
            Dinheiro valorNovo) {
        transitar(autor, acao, quando, observacao, valorAnterior, valorNovo, null);
    }

    private void transitar(String autor, AcaoProcesso acao, Instant quando, String observacao, Dinheiro valorAnterior,
            Dinheiro valorNovo, Dinheiro alcada) {
        Transicao transicao = new Transicao(estado, tipo.fluxo().proximo(estado, acao, contexto(alcada)), acao, quando,
                autor, observacao, valorAnterior, valorNovo);
        estado = transicao.para();
        novasTransicoes.add(transicao);
    }

    /**
     * Confere o autor contra a linha da tabela do fluxo (estado atual, ação): precisa ter um dos papéis da linha (R18) e,
     * se a linha exige (R17), não ser quem concluiu a análise pela última vez. Quem decide é a tabela, não esta raiz.
     */
    private void exigirPermissao(Autor autor, AcaoProcesso acao) {
        if (autor == null) {
            throw new DominioException("Autor da ação é obrigatório");
        }
        Set<Papel> papeis = tipo.fluxo().papeisDe(estado, acao);
        if (!autor.temAlgumDosPapeis(papeis)) {
            throw new PermissaoNegadaException("Para " + acao.descricao() + " um processo no estado " + estado
                    + " é preciso ter o papel " + papeis.stream().map(p -> p.name().toLowerCase()).sorted()
                            .collect(Collectors.joining(" ou ")));
        }
        if (tipo.fluxo().exigeOutraPessoaQueConcluiuAAnalise(estado, acao)
                && autor.usuario().equals(autorDaUltimaConclusaoDaAnalise)) {
            throw new PermissaoNegadaException(
                    "Quem concluiu a análise não pode " + acao.descricao() + " o mesmo processo (R17)");
        }
    }

    /**
     * A linha da tabela, no estado atual, é do cliente da cota (alterar, concordar, discordar): o caso de uso confere
     * então que o cliente do autor é o dono da cota (R18). Falso se a ação não vale no estado (a recusa será o 409).
     */
    public boolean exigeClienteDaCota(AcaoProcesso acao) {
        try {
            return tipo.fluxo().papeisDe(estado, acao).contains(Papel.CLIENTE);
        } catch (TransicaoInvalidaException e) {
            return false;
        }
    }

    /** Confere se a ação vale no estado atual, sem aplicá-la; para validar antes de um efeito externo. */
    public void exigirQuePode(AcaoProcesso acao) {
        tipo.fluxo().proximo(estado, acao, contexto(null));
    }

    /** O que a tabela do fluxo precisa para escolher o destino: o valor vigente agora e a alçada informada. */
    private ContextoDaTransicao contexto(Dinheiro alcada) {
        return new ContextoDaTransicao(valor, alcada);
    }

    /** Ações que o fluxo do tipo aceita no estado atual. */
    public Set<AcaoProcesso> acoesDisponiveis() {
        return tipo.fluxo().acoesDisponiveis(estado);
    }

    /** Transições feitas desde que o processo foi criado ou lido; o repositório as grava no histórico ao salvar. */
    public List<Transicao> novasTransicoes() {
        return List.copyOf(novasTransicoes);
    }

    public boolean ativo() {
        return !estado.finalizado();
    }

    public Long id() {
        return id;
    }

    public Long cotaId() {
        return cotaId;
    }

    public TipoProcesso tipo() {
        return tipo;
    }

    public Dinheiro valor() {
        return valor;
    }

    /** Alteração proposta aguardando a resposta do cliente; só existe em Aguardando concordância. */
    public Optional<AlteracaoProposta> propostaPendente() {
        return Optional.ofNullable(proposta);
    }

    /** Favorecidos vigentes, na ordem em que foram informados; só leitura. */
    public List<Favorecido> favorecidos() {
        return favorecidos;
    }

    public EstadoProcesso estado() {
        return estado;
    }

    /** Usuário que concluiu a análise pela última vez (R17); vazio se ela nunca foi concluída. */
    public Optional<String> autorDaUltimaConclusaoDaAnalise() {
        return Optional.ofNullable(autorDaUltimaConclusaoDaAnalise);
    }

    public String protocoloPagamento() {
        return protocoloPagamento;
    }

    /** Identificação da solicitação em andamento; só existe em Pagamento solicitado. */
    public Optional<IdentificacaoSolicitacao> identificacaoSolicitacao() {
        return Optional.ofNullable(identificacaoSolicitacao);
    }

    /** Versão gravada quando o processo foi lido; a gravação falha se outra operação já a mudou. */
    public long versao() {
        return versao;
    }
}
