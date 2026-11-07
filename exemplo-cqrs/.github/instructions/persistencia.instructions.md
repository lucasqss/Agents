---
applyTo: "**/persistence/**"
---
# Persistência (nosso banco)

`shared/persistence`: entidades JPA (`*Entity`). Só campos, anotações de mapeamento, getters e setters. Nenhum outro método (o ArchUnit falha). Sem herança JPA.

`command/persistence`: implementação dos repositórios do domínio, agrupada em `repository/`, `mapper/` e `adapter/`.
- `repository/XDataRepository`: interface Jakarta Data `@Repository` com `@Find` (buscar por id), `@Insert`, `@Update` e `@Query` em HQL para consultas e exclusão. O HQL é validado na compilação.
- `adapter/XRepositoryAdapter implements XRepository` (interface do domínio) e `mapper/XMapper` escrito à mão (`public static paraDominio` e `paraEntidade`). Sem MapStruct.
- Mapper e `XDataRepository` são públicos só porque estão em pacotes diferentes: **só o adapter os usa** (CMD-011, CMD-012).
- Stateless session: sem dirty checking. Para gravar, monte a entidade com `XMapper.paraEntidade(dominio)` e chame `inserir` (id nulo) ou `atualizar`. Alterar uma entidade carregada não grava nada.
- Ninguém fora deste pacote o referencia: o acesso é sempre pela interface do domínio.

`query/persistence`: repositórios Jakarta Data em `repository/` (`@Repository`) com `@Query` em HQL projetando direto em `record` de `projection/` (`select new ...Record(...)`).
- O HQL é validado na compilação pelo Hibernate Processor; nada de SQL nativo.
- O record mora em `projection/`. Não depende de `query/application` nem de nenhum canal.

Mudança de schema: nova migration `nucleo/src/main/resources/db/migration/V<n+1>__descricao.sql`. Nunca edite uma migration existente.
