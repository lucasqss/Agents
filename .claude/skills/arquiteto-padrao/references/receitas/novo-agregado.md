# Receita: novo agregado

Criar agregado é decisão do dono ([quando parar](../../SKILL.md#quando-parar-e-pedir-ao-dono)). Entrada necessária: o nome no vocabulário do negócio, o que ele guarda, a regra que ele protege e a primeira operação.

**É mesmo um agregado novo?** É, se tem identidade e ciclo de vida próprios e pode ser alterado sem carregar outro. Se só existe dentro de outro e muda junto com ele, é filho ou Value Object do agregado existente ([domínio](../dominio.md), [valores](../valores.md)).

1. **Domínio** (`domain/<agregado>/`): entidade com construtor privado e fábricas `novo` e `reconstituir`, validando as invariantes; interface `XRepository` só com o que a primeira operação usa. Referência a outro agregado é o id. Teste JUnit puro das invariantes, inclusive as do conjunto de filhos, se houver.
   - Filhos Value Object: record por item e lista recebida na fábrica ([domínio](../dominio.md)); sem repositório próprio, e a tabela filha entra na migration do passo 2 com FK, ordem e `unique` da não repetição.
2. **Banco:** migration `V<n+1>__...sql` com a tabela, as restrições (`not null`, `unique`, FK) e a coluna de versão se o agregado é alterado ([concorrência](../concorrencia.md)).
3. **Persistência** (`persistence/<agregado>/`): `XEntity`, `XDataRepository`, `XMapper`, `XRepositoryAdapter` ([persistência](../persistencia-jakarta-data.md)). Teste de ida e volta do mapper.
4. **Primeira operação:** [nova operação de escrita](novo-caso-de-uso-escrita.md), que cria `application/<agregado>/` e `entrypoint/rest/<agregado>/`.
5. **Consulta mínima** para conferir o que foi gravado: [nova consulta](nova-consulta.md).
6. `package-info.java` em cada pasta nova, se o projeto os usa nos agregados existentes.
7. `./mvnw verify`.

Crie só as pastas que a primeira operação usa. Canal, gateway e subpastas do agregado entram quando houver o que colocar nelas.
