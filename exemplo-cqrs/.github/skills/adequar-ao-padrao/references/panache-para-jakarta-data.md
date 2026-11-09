# Panache → Jakarta Data

O destino está em `references/persistencia-jakarta-data.md` da skill `arquiteto-padrao`. Aqui fica só a conversão. Faça depois de [separar a entidade do domínio](entidade-jpa-e-dominio.md).

No `pom.xml`, troque a extensão Panache pela dependência `jakarta.data-api` e inclua `hibernate-processor` nos `annotationProcessorPaths`, como no projeto modelo.

## Tabela de conversão

| Panache | Jakarta Data |
|---|---|
| `extends PanacheEntity` | `*Entity` simples, com `@Id` explícito (mesma coluna e geração) |
| `implements PanacheRepository<X>` | interface `@Repository` package-private |
| `X.findById(id)` | `@Find Optional<XEntity> buscarPorId(Long id)` |
| `x.persist()` | `@Insert void inserir(XEntity x)` |
| alterar campo e deixar o flush gravar | `@Update void atualizar(XEntity x)` |
| `X.deleteById(id)` | `@Query("delete from XEntity x where x.id = :id")` |
| `X.find("numero", valor)` | `@Query` com HQL completo |
| `X.find("status = ?1", s).page(...)` | `@Query` + `PageRequest`, devolvendo `Page<...>` |
| `X.count("cliente", c)` | `@Query("select count(x) from XEntity x where ...")` |
| `X.listAll()` em tela | record projetado + `@Query` em `query/persistence` |

O atalho do Panache (`"numero"`, `"status = ?1"`) não existe em Jakarta Data: escreva o HQL completo, com parâmetros nomeados. Ele passa a ser validado na compilação, e um erro antes escondido pode aparecer aqui.

## A armadilha principal: nada grava sozinho

Com stateless session, **alterar uma entidade carregada não grava nada, e o código continua compilando e rodando.** Antes de converter, procure todos os pontos que dependem de:

| Comportamento do Panache/ORM | O que fazer |
|---|---|
| Dirty checking (altera e o flush grava) | `@Update` explícito no adapter |
| Cascade | o adapter grava e apaga os filhos, na transação do use case |
| Orphan removal | o adapter apaga os removidos |
| Lazy loading | carregar explicitamente, ou trazer na própria consulta |
| Callbacks (`@PrePersist`) | código explícito no domínio ou no adapter |
| Cache de primeiro nível (mesmo objeto na transação) | não contar com isso; recarregar ou passar o objeto |

Para cada operação de escrita, um teste precisa **gravar, reler do banco e conferir o valor**. É o único jeito de pegar um `@Update` esquecido.

Se o projeto usa `flush()`, `EntityManager` direto ou locks (`LockModeType`), pare e pergunte ao dono.
