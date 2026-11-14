# Estilo e proibidos

## Proibido

- Uma classe por estado, um método por ação na entidade ou `if (tipo == ...)` para decidir transição. O ciclo de vida é enum + tabela em `domain/<agregado>/fluxo/`.
- Pastas `shared`, `common`, `util`, `command` ou `query`.
- `jakarta.validation` (Bean Validation) fora de `entrypoint/`.
- `domain/` importar JPA, Jakarta Data, CDI (`@Inject`, `@ApplicationScoped`), JAX-RS, Bean Validation, Quarkus, Hibernate ou MicroProfile.
- `domain/<agregado>/` importar outro agregado.
- Caso de uso importar JPA, Jakarta Data, `*Entity` ou as implementações de `persistence/`.
- Caso de uso de escrita usar `*QueryRepository`. Caso de uso de leitura usar `domain/`.
- `*UseCase` com mais de um método público.
- Panache. A persistência é só Jakarta Data; gravar é sempre explícito (`@Insert`/`@Update`), sem dirty checking.
- `*Entity` com qualquer método além de getter e setter.
- Lombok, MapStruct, `@Query(nativeQuery)` ou SQL nativo. Mappers são escritos à mão; consultas são HQL validado na compilação.
- `double` ou `float` para dinheiro, e conta com `BigDecimal` solto no domínio. Valor monetário é `Dinheiro` ([valores](valores.md)).
- Chamar sistema externo com efeito sem chave de idempotência ([concorrência](concorrencia.md)).
- Herança JPA. Uma tabela por agregado; subtipos são reconstruídos pelo mapper.
- DTO ou client de sistema externo fora de `gateway/<sistema>/`. Gateway nunca importa o domínio; a única exceção é o `XGatewayAdapter` ([gateway](gateway.md)).
- Classe do catálogo corporativo (dependência Maven gerada de outro sistema) fora de `gateway/` e `entrypoint/`. Para o caso de uso, o Gateway devolve tipo simples ou record próprio ([gateway](gateway.md)).
- `@RegisterRestClient`, `@Timeout`, `@Retry`, `@CircuitBreaker` fora de `gateway/`.
- Resource de canal devolver record de `persistence/`; ele devolve o próprio `*Response`.
- Canal usar o `Padrao` de outro canal; formatação fora de `entrypoint/`.
- Renomear ou remover campo de um `*Response` de canal. Só mudanças aditivas; quebra vira `/v2`.
- Atualizar `docs/api/openapi-base.json` para o build passar depois de uma mudança que não é aditiva. A linha de base só muda junto com uma mudança aditiva (campo novo, endpoint novo, `/v2`).
- `@SuppressWarnings("NullAway")`, ou tirar pacote de `nullaway.pacotes` / mudar a configuração do NullAway, para o build passar.
- Dependência proibida contornada no Enforcer, ou aviso do ErrorProne silenciado sem uma justificativa escrita ao lado.

## Estilo

- Java moderno: `record` para dados imutáveis; `enum` + tabela de transições para ciclo de vida ([dominio](dominio.md)); `sealed` + `switch` com pattern matching quando os **dados** mudam por tipo.
- Injeção por construtor. Assemblers e `Padrao*` são classes puras, sem CDI.
- Nomes em português no domínio e nos casos de uso, seguindo o vocabulário já existente.
- Exceções e seus códigos HTTP: veja [canais](canais.md).

## Nulos

O NullAway confere na compilação: o que não está anotado não pode ser nulo.

- **Pode faltar de verdade:** declare com `org.jspecify.annotations.Nullable`, no tipo (`private @Nullable Long id;`, `public @Nullable String protocolo()`, `@Nullable Dinheiro alcada` no record). Vale também no domínio: é só anotação, sem código em tempo de execução. Exemplos: id antes de persistir, coluna opcional, campo que só existe em um estado.
- **Uma invariante garante o valor:** `Objects.requireNonNull(valor, "por quê")` no ponto de uso, não `@Nullable` espalhado. Exemplo: em Aguardando concordância a proposta sempre existe.
- **Entidade JPA:** só os campos das colunas que aceitam nulo levam `@Nullable` (campo, getter e setter), e `setId(@Nullable Long id)`.
- **"Valor presente devolve valor":** `@Contract("!null -> !null")` (de `org.jetbrains.annotations`) no método e **também na implementação concreta**, porque o contrato não é herdado. Exemplo: os `Padrao*` de canal.
- **`Optional`** continua sendo o retorno de busca; `@Nullable` é para campo, parâmetro e retorno de acessor.
- **Request do entrypoint:** o Bean Validation garante o formato; não anote os componentes do `*Request` com `@Nullable` só porque o Jackson poderia mandar nulo.
