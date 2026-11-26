# Persistência com Jakarta Data (`persistence/<agregado>/`)

Sem Panache, sem SQL nativo. O HQL é validado na compilação pelo Hibernate Processor.

**Stateless session:** não há dirty checking, cascade nem lazy loading. Alterar uma entidade carregada não grava nada. Gravar é sempre `@Insert` ou `@Update` explícito, e os filhos são gravados e carregados explicitamente pelo adapter.

Uma pasta, dois caminhos:

| Caminho | Classes | Quem usa |
|---|---|---|
| Escrita | `XEntity`, `XDataRepository`, `XMapper`, `XRepositoryAdapter` | caso de uso de escrita, pela interface do domínio |
| Leitura de tela | `XQueryRepository`, records projetados | caso de uso de leitura, direto |

## Entidade JPA (`XEntity`)

- Só campos, anotações de mapeamento, getters e setters. Nenhum outro método (o ArchUnit falha). Sem herança JPA, sem construtor com lógica, sem Bean Validation, sem callbacks.
- Sem associações JPA para outros agregados: a FK é um campo simples (`@Column(name = "cliente_id") Long clienteId`).
- Filho do agregado é a própria `*Entity` (tabela filha), com a coluna de FK, uma coluna de ordem se a ordem importa e `unique` que espelha a invariante de não repetição do domínio. `@Embedded` fica com as colunas achatadas ou num `@Embeddable` no mesmo pacote.
- Enum é `String` (ou `@Enumerated(EnumType.STRING)`); a conversão é do mapper.

## Escrita

Implementa a interface de repositório do domínio. O acesso é sempre pela interface: fora deste pacote ninguém referencia `XDataRepository`, `XMapper` nem `XRepositoryAdapter`.

**`XDataRepository`:** interface Jakarta Data, package-private.

```java
@Repository
interface XDataRepository {

    @Find
    Optional<XEntity> buscarPorId(Long id);

    @Insert
    void inserir(XEntity x);

    @Update
    void atualizar(XEntity x);

    @Query("delete from XEntity x where x.id = :id")
    void excluir(Long id);

    @Query("select count(x) from XEntity x where x.numero = :numero and (:ignorarId is null or x.id <> :ignorarId)")
    long contarPorNumero(String numero, Long ignorarId);
}
```

**`XMapper`:** classe `final`, package-private, métodos estáticos `paraDominio(entity)` e `paraEntidade(dominio)`, escrita à mão. `paraDominio` usa `reconstituir(...)`. Valor inválido vindo do banco (enum desconhecido) lança `IllegalStateException` com o valor na mensagem. Tipo e estado são reconstruídos pelo `valueOf` do enum, sem `switch` no mapper ([exemplo](exemplos-codigo.md)).

**`XRepositoryAdapter`** ([exemplo](exemplos-codigo.md)): `@ApplicationScoped`, `implements XRepository`, injeção por construtor do `XDataRepository`. `salvar`: `paraEntidade`, depois `inserir` se o id for nulo ou `atualizar` se não, e devolve `paraDominio(entity)`. Com filhos: grave o agregado, depois os filhos (apague os removidos), tudo na transação do caso de uso. Filho Value Object: `salvar` grava os itens da raiz (na ordem, preenchendo a coluna de ordem) e `reconstituir` os carrega na mesma ordem; a consulta dos filhos para tela vai em `XQueryRepository`. Histórico de transições: o adapter insere uma linha por item de `novasTransicoes()` do agregado; nunca altera nem apaga as antigas.

- **Troca de filhos** (filhos Value Object que podem mudar, como a proposta pendente): o `salvar` confere a versão antes de tudo, e quem perdeu a disputa falha sem tocar nos filhos.
  - Compara os filhos gravados com os do agregado, e só apaga e regrava quando mudaram.
  - Veja `trocarFavorecidosSeMudaram` e `trocarPropostaSeMudou` no adapter do exemplo.
- **Leitura sem bloqueio e agregado incoerente:** raiz e filhos vêm de consultas separadas. Na leitura sem bloqueio, outra transação pode confirmar entre elas, o conjunto fica incoerente, e a recusa do domínio ao montar o agregado vira `AlteracaoConcorrenteException` (409), com a recusa original como causa.
  - Na leitura com bloqueio, e logo após gravar, os dados são coerentes: a mesma recusa é dado inválido no banco e sai como `IllegalStateException`. Veja `reconstituir(entity, leituraSemBloqueio)` no exemplo.

**Versão:** agregado que é alterado tem coluna de versão e `@Version` na `*Entity`; o mapper leva e traz a versão, e o adapter converte a falha de gravação concorrente na exceção do domínio. Quando usar versão e quando usar bloqueio: [concorrência](concorrencia.md).

**Bloqueio pessimista:** use quando duas requisições simultâneas não podem passar juntas por uma verificação ("já existe processo em andamento?"). O Jakarta Data não expressa bloqueio, então declare `StatelessSession sessao();` no `XDataRepository` e um método `default` com `sessao().get(XEntity.class, id, LockMode.PESSIMISTIC_WRITE)` ([exemplo](exemplos-codigo.md)). Exponha no domínio como `buscarPorIdComBloqueio`, para o caso de uso não conhecer JPA nem Hibernate. O bloqueio dura até o fim da transação do caso de uso.

## Leitura de tela (`XQueryRepository`)

- Repositórios Jakarta Data (`@Repository`) com `@Query` em HQL projetando direto em `record` (`select new <pacote>.XRecord(...)`).
- O record mora aqui, junto do repositório. Não depende de `application/` nem de `entrypoint/`.
- Lista grande usa `PageRequest` e devolve `Page<Record>`.
- O HQL pode juntar `*Entity` de qualquer agregado do componente. O arquivo fica no agregado principal da consulta.

Mudança de schema: veja [migrations](migrations.md).
