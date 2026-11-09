# Estilo e proibidos

## Proibido

- `jakarta.validation` (Bean Validation) fora do entrypoint.
- `domain` importar JPA, Jakarta Data, CDI (`@Inject`, `@ApplicationScoped`), JAX-RS, Bean Validation, Quarkus, Hibernate ou MicroProfile.
- `query` importar qualquer coisa de `command`, e `command` importar qualquer coisa de `query`.
- Use case importar JPA, Jakarta Data, `*Entity` ou a implementação em `persistence`.
- Panache. A persistência é só Jakarta Data; gravar é sempre explícito (`@Insert`/`@Update`), sem dirty checking.
- `*Entity` com qualquer método além de getter e setter.
- Lombok, MapStruct, `@Query(nativeQuery)` ou SQL nativo. Mappers são escritos à mão; consultas são HQL validado na compilação.
- Herança JPA. Uma tabela por agregado; subtipos são reconstruídos pelo mapper.
- DTO ou client de sistema externo fora de `gateway/<sistema>`. Gateway nunca importa o domínio.
- `@RegisterRestClient`, `@Timeout`, `@Retry`, `@CircuitBreaker` fora de `gateway`.
- Resource de canal devolver record de `persistence`; ele devolve o próprio `*Response`.
- Canal usar o `Padrao` de outro canal; formatação fora do entrypoint.
- Renomear ou remover campo de um `*Response` de canal. Só mudanças aditivas; quebra vira `/v2`.

## Estilo

- Java moderno: `record` para dados imutáveis, `sealed` + `switch` com pattern matching onde há estados fechados.
- Injeção por construtor. Assemblers e `Padrao*` são classes puras, sem CDI.
- Nomes em português no domínio e nos use cases, seguindo o vocabulário já existente.
- Exceções e seus códigos HTTP: veja [canais](canais.md).
