# Receita: novo estado, ação ou tipo em um fluxo

Entradas necessárias: o agregado e a tabela do que muda (de qual estado, por qual ação, para qual estado) e, se o fluxo varia por tipo, em quais tipos. Se faltar, pergunte. Modelos em [exemplos de código](../exemplos-codigo.md); regras em [dominio](../dominio.md).

Tudo fica em `domain/<agregado>/fluxo/` e `domain/<agregado>/tipo/`. A entidade e o caso de uso que executa ações não mudam.

| Quero... | Faça |
|---|---|
| Transição nova entre estados e ação que já existem | uma linha `.transicao(de, acao, para)`: em `comum()` se vale para todos os tipos, senão no `Fluxo<Tipo>` |
| Estado novo | constante no enum de estados (dizendo se é final) + as linhas que chegam nele e saem dele + rótulo em `Rotulos` |
| Ação nova que só muda o estado | constante no enum de ações + as linhas que a aceitam + endpoint que chama o caso de uso de executar ação |
| Ação nova que exige um dado de quem pede (motivo, por exemplo) | o mesmo, mais o atributo no enum de ações, a recusa na raiz do agregado e um record de request com Bean Validation no endpoint. Segue no caso de uso genérico de executar ação |
| Ação nova que carrega dado próprio (novo valor, nova lista de favorecidos) | o mesmo, mais `comMetodoProprio` no enum de ações e um método na raiz que confere o dado e usa a mesma tabela. Tem caso de uso próprio, porque o executar genérico a recusa ([domínio](../dominio.md)) |
| Ação nova que só a rotina aplica (confirmar, recusar, tentar de novo) | o mesmo, mais a marca no enum de ações: fora de `acoesDisponiveis` e do executar genérico ([domínio](../dominio.md)). Se o estado novo é exceção a uma regra geral da tabela (não aceita cancelar), declare-a na construção da tabela |
| Ação nova que chama sistema externo | o mesmo, mais um método na entidade que confere o dado e um caso de uso próprio (receita [novo caso de uso de escrita](novo-caso-de-uso-escrita.md)) |
| Tipo novo | constante no enum do tipo + `Fluxo<Tipo>` partindo de `comum()` com os desvios + rótulo em `Rotulos` |

Passos:

0. **A ação exige dado?** Se sim: a regra vai no enum de ações e a raiz do agregado recusa sem o dado ([domínio](../dominio.md)); o formato (obrigatório, não vazio, tamanho) vai no request do endpoint, com corpo obrigatório ([canais](../canais.md)); o dado entra no histórico. Teste: domínio recusa sem o dado, API responde 400 com o nome do campo. Se não, o endpoint aceita requisição sem corpo.
1. Escreva antes o teste JUnit puro do fluxo: cada linha nova leva ao destino, e a ação continua recusada (`TransicaoInvalidaException`) nos estados em que não vale.
2. Altere os enums e as tabelas conforme a linha da tabela acima.
3. **Banco:** estado e tipo são gravados pelo nome da constante. Constante nova não pede migration, a não ser que a coluna tenha `CHECK` com a lista de valores. Nunca renomeie uma constante já gravada.
4. **Canais:** estado ou tipo novo ganha rótulo em `entrypoint/apresentacao/Rotulos` e aparece nos testes de contrato.
5. Teste de API cobrindo o caminho novo até o estado final e uma recusa (409).
6. Rode `./mvnw verify`.
