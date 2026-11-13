# Receita: novo estado, ação ou tipo em um fluxo

Entradas necessárias: o agregado e a tabela do que muda (de qual estado, por qual ação, para qual estado) e, se o fluxo varia por tipo, em quais tipos. Se faltar, pergunte. Modelos em [exemplos de código](../exemplos-codigo.md); regras em [dominio](../dominio.md).

Tudo fica em `domain/<agregado>/fluxo/` e `domain/<agregado>/tipo/`. A entidade e o caso de uso que executa ações não mudam.

| Quero... | Faça |
|---|---|
| Transição nova entre estados e ação que já existem | uma linha `.transicao(de, acao, para)`: em `comum()` se vale para todos os tipos, senão no `Fluxo<Tipo>` |
| Estado novo | constante no enum de estados (dizendo se é final) + as linhas que chegam nele e saem dele + rótulo em `Rotulos` |
| Ação nova que só muda o estado | constante no enum de ações + as linhas que a aceitam + endpoint que chama o caso de uso de executar ação |
| Ação nova que precisa de dado ou chama sistema externo | o mesmo, mais um método na entidade que confere o dado e um caso de uso próprio (receita [novo caso de uso de escrita](novo-caso-de-uso-escrita.md)) |
| Tipo novo | constante no enum do tipo + `Fluxo<Tipo>` partindo de `comum()` com os desvios + rótulo em `Rotulos` |

Passos:

1. Escreva antes o teste JUnit puro do fluxo: cada linha nova leva ao destino, e a ação continua recusada (`TransicaoInvalidaException`) nos estados em que não vale.
2. Altere os enums e as tabelas conforme a linha da tabela acima.
3. **Banco:** estado e tipo são gravados pelo nome da constante. Constante nova não pede migration, a não ser que a coluna tenha `CHECK` com a lista de valores. Nunca renomeie uma constante já gravada.
4. **Canais:** estado ou tipo novo ganha rótulo em `entrypoint/apresentacao/Rotulos` e aparece nos testes de contrato.
5. Teste de API cobrindo o caminho novo até o estado final e uma recusa (409).
6. Rode `./mvnw verify`.
