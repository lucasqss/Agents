package br.com.claudio.consorcio.domain.processo.estado;

/**
 * Padrão: State, estado concreto [GoF].
 * Etapa de vistoria, usada só pelos tipos cujo fluxo passa por ela.
 */
public record AguardandoVistoria() implements EstadoProcesso {

    public static final String NOME = "AGUARDANDO_VISTORIA";

    @Override
    public String nome() {
        return NOME;
    }

    @Override
    public EstadoProcesso concluirVistoria() {
        return new Aprovado();
    }
}
