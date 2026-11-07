package br.com.claudio.consorcio.shared.identidade;

/**
 * Quem fez a requisição, na forma comum a todos os canais. Cada canal lê o PRÓPRIO token (formato e
 * biblioteca corporativa dele) e o traduz para este record; o núcleo nunca sabe como o token era.
 *
 * @param id     identificador do usuário ou do equipamento no canal (matrícula, CPF, terminal...)
 * @param canal  nome do canal de atendimento (WEB, MOBILE, TAA...)
 */
public record Solicitante(String id, String canal) {

    public Solicitante {
        if (id == null || id.isBlank() || canal == null || canal.isBlank()) {
            throw new IllegalArgumentException("Solicitante precisa de id e canal");
        }
    }
}
