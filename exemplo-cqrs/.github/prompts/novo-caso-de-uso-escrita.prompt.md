---
mode: agent
description: Cria uma operação de escrita (regra no domínio, use case, endpoint e testes) seguindo a arquitetura do projeto.
---
Crie a operação de escrita descrita abaixo seguindo `.github/copilot-instructions.md`.

Operação: ${input:operacao:Descreva a operação, ex.: "suspender a cota quando houver atraso"}

Siga esta ordem e pare se algum passo exigir quebrar uma regra:
1. **Domínio** (`command/domain`): implemente a regra como método da entidade de domínio (ou novo estado, se for transição). Lance `DominioException`/`TransicaoInvalidaException` quando a regra for violada. Escreva antes os testes JUnit puros em `src/test/java/**/command/domain/`.
2. **Repositório**: se precisar de uma consulta nova para a escrita, acrescente o método na interface do domínio e implemente em `command/persistence` (Panache + mapper à mão).
3. **Banco**: se o estado novo precisar ser gravado, crie a migration `V<n+1>__...sql`, o campo na `*Entity` e ajuste o mapper.
4. **Use case** (`command/application`): `@Transactional`, busca -> método de negócio -> salvar. Sem regra de negócio aqui.
5. **Entrypoint** (`command/entrypoint`): `XRequest` com Bean Validation (formato, tamanhos iguais às colunas) e endpoint `@Valid @NotNull XRequest` que chama o use case. Exceções novas entram em `ExceptionMappers`.
6. **Teste de API**: `@QuarkusTest` + RestAssured cobrindo o caminho feliz e o erro principal.
7. Rode `./mvnw verify`. Se uma regra ArchUnit falhar, corrija o código, nunca a regra.

No final, liste os arquivos criados ou alterados por camada.
