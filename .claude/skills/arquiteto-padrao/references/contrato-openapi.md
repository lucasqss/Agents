# Contrato OpenAPI que o front consegue usar

O front gera os tipos do cliente a partir do OpenAPI publicado, com o `openapi-typescript` sobre `docs/api/openapi-base.json`. O que não está no contrato o front não consegue tipar, e o agente do front passa a adivinhar (status, formato de erro, campos que podem vir nulos). Por isso o contrato descreve **tudo o que o endpoint devolve**, não só o caminho feliz.

## Regras

1. **`operationId` estável:** `quarkus.smallrye-openapi.operation-id-strategy=METHOD` no `application.properties`. O nome do método do resource vira o nome da operação (`abrir`, `consultar`, `executar`), por isso os nomes de método não se repetem na API.
2. **Todo status de erro que o endpoint devolve é declarado,** com o corpo `ErroResponse`:

   | O endpoint... | Declare |
   |---|---|
   | recebe corpo, parâmetro ou cabeçalho com Bean Validation, ou o domínio pode recusar com `DominioException` | 400 |
   | busca um agregado ou uma referência pelo id | 404 |
   | executa ação que depende do estado atual, ou grava com versão | 409 |
   | é protegido por papel | 401 e 403 |

   ```java
   @APIResponse(responseCode = "200", description = "OK") // sem content: o SmallRye preenche pelo tipo de retorno
   @APIResponse(responseCode = "404", description = "Processo não encontrado",
           content = @Content(mediaType = JSON, schema = @Schema(implementation = ErroResponse.class)))
   ```

   **Ao declarar qualquer `@APIResponse`, declare também a de sucesso.** Sem ela o SmallRye deixa de gerar a resposta automática: o 200 some do contrato, e o openapi-diff acusa quebra. Com `content`, só quando o tipo não sai do retorno (ex.: `Response` com 201).
3. **O corpo da requisição é publicado como `application/json`:** `@RequestBody(content = @Content(mediaType = JSON, schema = @Schema(implementation = XRequest.class)))`. Se o endpoint também aceita requisição sem corpo e sem `Content-Type` (`@Consumes(MediaType.WILDCARD)`, ação sem dado), publique os dois tipos, JSON e `*/*`. Tirar o `*/*` do que já foi publicado quebra o contrato.
4. **Campo de resposta que pode vir nulo:** `@Nullable` (para o NullAway) **e** `@Schema(nullable = true)` (para o contrato). Os outros campos sempre vêm no JSON, porque o Jackson escreve as chaves, e o front gera com `--properties-required-by-default`. Assim, `string | null` só aparece onde o nulo existe de fato.
5. **Campo de resposta com valor de enum é publicado como o enum:** o campo do record tem o tipo do enum, ou, se a projeção traz `String`, leva `@Schema(implementation = XEnum.class)`. Assim o front tipa o vocabulário pela lista exata e o build dele quebra quando surge um valor novo.
6. **Cabeçalho obrigatório** (identidade, autor) entra como `@HeaderParam` com Bean Validation e aparece no contrato como parâmetro obrigatório.

## Campo já publicado não muda de tipo

As regras 4 e 5 valem **desde a primeira versão** do campo. Num campo já publicado, acrescentar `null` ao tipo ou trocar `string` por enum é mudança de tipo: o `ContratoRestTest` acusa, e o caminho é uma versão nova do endpoint (`/v2`), não editar o teste nem a linha de base. Declarar status de erro, `operationId`, tipos de conteúdo novos e schemas novos é aditivo, e o openapi-diff aceita.

## Como conferir

1. `./mvnw verify`: o openapi-diff termina com "API changes are backward compatible", e o `ContratoRestTest` passa.
2. Mudança aditiva: copie `target/openapi/openapi.json` para `docs/api/openapi-base.json` no mesmo commit.
3. Veja o que o front vai receber:

   ```bash
   npx openapi-typescript docs/api/openapi-base.json --properties-required-by-default -o /tmp/esquema.d.ts
   ```

   Em `/tmp/esquema.d.ts`, confira:
   - cada operação tem os status de erro com `ErroResponse`;
   - o corpo aparece como `application/json`;
   - os enums são uniões de literais;
   - `| null` aparece só nos campos anuláveis.
