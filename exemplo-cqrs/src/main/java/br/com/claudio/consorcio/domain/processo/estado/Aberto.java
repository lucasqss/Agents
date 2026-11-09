package br.com.claudio.consorcio.domain.processo.estado;

/**
 * Padrão: State, estado concreto [GoF].
 * Processo recém-criado; só aceita iniciar a análise (ou cancelar).
 */
public record Aberto() implements EstadoProcesso {

    public static final String NOME = "ABERTO";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso iniciarAnalise() {
        return new EmAnalise();
    }
}
