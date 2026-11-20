package cucumber.massa;

import cucumber.passos.Vocabulario;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A massa de processos da aceitação: {@code cucumber/massa/processos.md}, uma tabela que o QA preenche com os termos do
 * negócio e a cota como chave. Valida o arquivo inteiro antes de gravar qualquer linha, e grava com IDs determinísticos
 * (a sentinela é o 1; as linhas do arquivo, 2, 3... na ordem do arquivo; o que os cenários criarem, 1000 em diante).
 * Os cenários citam a cota e chegam ao ID por {@link #idDaCota}, que relê o mesmo arquivo: carregador e passos não
 * dividem estado.
 */
public final class MassaDeProcessos {

    public static final String ARQUIVO = "cucumber/massa/processos.md";
    public static final String SENTINELA = "COTA-SENTINELA";
    public static final long ID_DA_SENTINELA = 1;
    public static final long PRIMEIRO_ID_GERADO = 1000;
    private static final int TAMANHO_MAXIMO_DA_COTA = 30;
    private static final List<String> COLUNAS = List.of("cota", "tipo", "valor", "situação");

    /** Um processo da massa, já convertido para os códigos da API. */
    public record Processo(long id, String cota, String tipo, BigDecimal valor, String situacao, String onde) {
    }

    private static volatile Map<String, Processo> porCota;

    private MassaDeProcessos() {
    }

    /** Grava a sentinela e a massa num banco recém-migrado, e faz as sequências recomeçarem em 1000. */
    public static void carregar(Connection banco) throws SQLException {
        Map<String, Processo> massa = processos();
        String insert = "insert into processo_pagamento (id, tipo, numero_cota, valor, estado) values (?, ?, ?, ?, ?)";
        try (PreparedStatement comando = banco.prepareStatement(insert)) {
            gravar(comando, ID_DA_SENTINELA, "SERVICO", SENTINELA, new BigDecimal("30000.00"), "ABERTO");
            for (Processo processo : massa.values()) {
                gravar(comando, processo.id(), processo.tipo(), processo.cota(), processo.valor(), processo.situacao());
            }
            comando.executeBatch();
        }
        try (Statement comando = banco.createStatement()) {
            comando.execute("alter table processo_pagamento alter column id restart with " + PRIMEIRO_ID_GERADO);
            comando.execute("alter table processo_transicao alter column id restart with " + PRIMEIRO_ID_GERADO);
        }
    }

    /** O processo da massa com esta cota; falha dizendo a cota se ela não está no arquivo. */
    public static Processo daCota(String cota) {
        Processo processo = processos().get(cota.strip());
        if (processo == null) {
            throw new IllegalArgumentException("A cota '" + cota + "' não está em " + ARQUIVO
                    + ". Cotas da massa: " + processos().keySet());
        }
        return processo;
    }

    public static long idDaCota(String cota) {
        return daCota(cota).id();
    }

    private static Map<String, Processo> processos() {
        Map<String, Processo> atual = porCota;
        if (atual == null) {
            atual = ler();
            porCota = atual;
        }
        return atual;
    }

    /** Lê e valida o arquivo inteiro; junta todos os erros numa mensagem só, linha a linha. */
    private static Map<String, Processo> ler() {
        Map<String, Processo> processos = new LinkedHashMap<>();
        List<String> erros = new ArrayList<>();
        Set<String> cotas = new HashSet<>();
        long id = ID_DA_SENTINELA + 1;
        for (TabelaMarkdown.Linha linha : TabelaMarkdown.ler(ARQUIVO, COLUNAS)) {
            String cota = linha.valor("cota");
            try {
                if (cota.isEmpty()) {
                    throw new IllegalArgumentException("cota vazia");
                }
                if (cota.length() > TAMANHO_MAXIMO_DA_COTA) {
                    throw new IllegalArgumentException("cota com mais de " + TAMANHO_MAXIMO_DA_COTA + " caracteres");
                }
                if (SENTINELA.equals(cota)) {
                    throw new IllegalArgumentException(SENTINELA + " é reservada (a sentinela do banco da aceitação)");
                }
                if (!cotas.add(cota)) {
                    throw new IllegalArgumentException("cota '" + cota + "' repetida");
                }
                processos.put(cota, new Processo(id++, cota,
                        Vocabulario.codigo(Vocabulario.TIPOS, "tipo", linha.valor("tipo")),
                        Vocabulario.emReais(linha.valor("valor")),
                        Vocabulario.codigo(Vocabulario.SITUACOES, "situação", linha.valor("situação")),
                        linha.onde()));
            } catch (IllegalArgumentException e) {
                erros.add(linha.onde() + ": " + e.getMessage());
            }
        }
        if (!erros.isEmpty()) {
            throw new IllegalStateException("Massa inválida:\n  " + String.join("\n  ", erros));
        }
        return Collections.unmodifiableMap(processos);
    }

    private static void gravar(PreparedStatement comando, long id, String tipo, String cota, BigDecimal valor,
            String situacao) throws SQLException {
        comando.setLong(1, id);
        comando.setString(2, tipo);
        comando.setString(3, cota);
        comando.setBigDecimal(4, valor);
        comando.setString(5, situacao);
        comando.addBatch();
    }
}
