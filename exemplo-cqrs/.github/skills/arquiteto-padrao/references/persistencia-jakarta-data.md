# Persistência com Jakarta Data

Sem Panache, sem SQL nativo. O HQL é validado na compilação pelo Hibernate Processor.

**Stateless session:** não há dirty checking, cascade nem lazy loading. Alterar uma entidade carregada não grava nada. Gravar é sempre `@Insert` ou `@Update` explícito, e os filhos são gravados e carregados explicitamente pelo adapter.

## Entidade JPA (`<modulo>/persistence/XEntity`)

- Só campos, anotações de mapeamento, getters e setters. Nenhum outro método (o ArchUnit falha). Sem herança JPA, sem construtor com lógica, sem Bean Validation, sem callbacks.
- Sem associações JPA para outros agregados: a FK é um campo simples (`@Column(name = "cliente_id") Long clienteId`).
- Filho do agregado é a própria `*Entity`, com a coluna de FK. `@Embedded` fica com as colunas achatadas ou num `@Embeddable` no mesmo pacote.
- Enum é `String` (ou `@Enumerated(EnumType.STRING)`); a conversão é do mapper.

## Escrita (`persistence`, ou `command/persistence`)

Implementa a interface de repositório do domínio. Ninguém fora deste pacote referencia estas classes: o acesso é sempre pela interface do domínio.

**`XDataRepository`:** interface Jakarta Data, package-private.

```java
@Repository
interface CotaDataRepository {

    @Find
    Optional<CotaEntity> buscarPorId(Long id);

    @Insert
    void inserir(CotaEntity cota);

    @Update
    void atualizar(CotaEntity cota);

    @Query("delete from CotaEntity c where c.id = :id")
    void excluir(Long id);

    @Query("select count(c) from CotaEntity c where c.numero = :numero and (:ignorarId is null or c.id <> :ignorarId)")
    long contarPorNumero(String numero, Long ignorarId);
}
```

**`XMapper`:** classe `final`, package-private, métodos estáticos `paraDominio(entity)` e `paraEntidade(dominio)`, escrita à mão. `paraDominio` usa `reconstituir(...)`. Valor inválido vindo do banco (enum desconhecido) lança `IllegalStateException` com o valor na mensagem. Tipo e estado são reconstruídos por `TipoProcesso.valueOf` e `EstadoProcesso.porNome`, sem `switch` no mapper (veja `ProcessoPagamentoMapper`).

**`XRepositoryAdapter`:** `@ApplicationScoped`, `implements XRepository`, injeção por construtor do `XDataRepository`. `salvar`: `paraEntidade`, depois `inserir` se o id for nulo ou `atualizar` se não, e devolve `paraDominio(entity)`. Com filhos: grave o agregado, depois os filhos (apague os removidos), tudo na transação do use case.

## Leitura (`query/persistence`)

- Repositórios Jakarta Data (`@Repository`) com `@Query` em HQL projetando direto em `record` (`select new <pacote>.XRecord(...)`).
- O record mora aqui, junto do repositório. Não depende de `query/application` nem de `query/entrypoint`.
- Lista grande usa `PageRequest` e devolve `Page<Record>`.
- HQL só usa `*Entity` do próprio módulo.

Mudança de schema: veja [migrations](migrations.md).
