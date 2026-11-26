# Receita: nova operação de escrita

Entrada necessária: a descrição da operação (ex.: "suspender a cota quando houver atraso"). Se faltar, pergunte.

Siga esta ordem e pare se algum passo exigir quebrar uma regra:

1. **Domínio** (`domain/<agregado>/`): implemente a regra como método da entidade de domínio. Se a operação é uma mudança de estado do ciclo de vida, não crie método nem caso de uso novo: siga a receita [novo estado, ação ou tipo](novo-estado-acao-ou-tipo.md). Lance `DominioException` quando a regra for violada. Escreva antes os testes JUnit puros. **A regra depende de outro agregado ou de um total?** Ela vai na raiz dona do limite, recebendo o total já calculado ([domínio](../dominio.md)); o caso de uso bloqueia essa raiz e lê o total ([casos de uso](../use-cases.md)).
2. **Repositório:** se precisar de uma consulta nova para a escrita, acrescente o método na interface em `domain/<agregado>/` e implemente em `persistence/<agregado>/` (método `@Query` em HQL no `XDataRepository` + adapter + mapper à mão).
3. **Banco:** se o estado novo precisar ser gravado, crie a migration `V<n+1>__...sql`, o campo na `*Entity` e ajuste o mapper.
4. **Caso de uso** (`application/<agregado>/XUseCase.java`): uma classe, um método público, `@Transactional`, busca → método de negócio → salvar. Sem regra de negócio aqui. Se a operação recebe dados, crie o record `application/<agregado>/DadosX.java`, sem Bean Validation, e receba-o no método público ([exemplo](../exemplos-codigo.md)).
5. **Entrypoint** (`entrypoint/rest/<agregado>/`): `XRequest` com Bean Validation (formato, tamanhos iguais às colunas) com método `paraDados()` que devolve o `DadosX`, e endpoint `@Valid @NotNull XRequest` que chama o caso de uso. Exceções novas entram em `entrypoint/ExceptionMappers`.
6. **Teste de API:** `@QuarkusTest` + RestAssured cobrindo o caminho feliz e o erro principal.
7. Rode `./mvnw verify`. Se uma regra ArchUnit falhar, corrija o código, nunca a regra.

No final, liste os arquivos criados ou alterados por camada.
