package br.com.claudio.consorcio.domain;

/**
 * A pessoa identificada não pode executar a ação: papel que a linha da tabela do fluxo não admite, cliente que não é o
 * dono da cota ou a mesma pessoa que concluiu a análise tentando aprovar (R17, R18). O entrypoint a traduz para 403.
 */
public class PermissaoNegadaException extends DominioException {

    public PermissaoNegadaException(String mensagem) {
        super(mensagem);
    }
}
