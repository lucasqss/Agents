---
applyTo: "**/persistence/**"
---
# Persistência (nosso banco)

`shared/persistence`: entidades JPA (`*Entity`). Só campos, anotações de mapeamento, getters e setters. Nenhum outro método (o ArchUnit falha). Sem herança JPA.

`command/persistence`: implementação dos repositórios do domínio.
- `XDataRepository`: interface Jakarta Data `@Repository` package-private com `@Find` (buscar por id), `@Insert`, `@Update` e `@Query` em HQL para consultas e exclusão. O HQL é validado na compilação.
- `XRepositoryAdapter implements XRepository` (interface do domínio) e `XMapper` escrito à mão (`paraDominio` e `paraEntidade`). Sem MapStruct.
- Stateless session: sem dirty checking. Para gravar, monte a entidade com `XMapper.paraEntidade(dominio)` e chame `inserir` (id nulo) ou `atualizar`. Alterar uma entidade carregada não grava nada.
- Ninguém fora deste pacote o referencia: o acesso é sempre pela interface do domínio.

`query/persistence`: repositórios Jakarta Data (`@Repository`) com `@Query` em HQL projetando direto em `record` (`select new ...Record(...)`).
- O HQL é validado na compilação pelo Hibernate Processor; nada de SQL nativo.
- O record mora aqui, junto do repositório. Não depende de `query/application` nem de `query/entrypoint`.

Mudança de schema: nova migration `src/main/resources/db/migration/V<n+1>__descricao.sql`. Nunca edite uma migration existente.
