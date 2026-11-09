# Casos de uso (`application/<agregado>/`)

O caso de uso é o Service Layer: orquestra e define a transação. Ele **não sabe qual canal o chamou** e não formata nada.

**Uma classe, uma operação, um único método público.** `CriarCotaUseCase`, `AtualizarCotaUseCase` e `ExcluirCotaUseCase` são três classes.

A pasta é a do agregado principal da operação. Excluir uma cota verifica processos, mas é operação de cota: fica em `application/cota/`.

## Escrita

- Classe `*UseCase`, `@ApplicationScoped`, injeção por construtor, `@Transactional` no método público.
- Roteiro: buscar o agregado pelo repositório do domínio (`orElseThrow(() -> new NaoEncontradoException(...))`), chamar o método de negócio, salvar.
- A regra fica no domínio. Se o caso de uso começar a ter `if` de negócio, mova para a entidade.
- Pode usar repositórios de vários agregados: é aqui que dois agregados se encontram.
- Pode chamar `*Gateway`. Faça a transição do domínio **antes** da chamada externa, para que uma transição inválida nem chegue ao sistema externo.
- Proibido importar `*Entity`, Jakarta Data, JPA, as implementações de `persistence/` ou qualquer `*QueryRepository`.

Precisa consultar algo para validar ou decidir a escrita?

1. Acrescente o método na interface de repositório do domínio, no vocabulário do negócio: `boolean existeProcessoAtivo(Long cotaId)`, `List<Cota> buscarPorCliente(Long clienteId)`.
2. Implemente no `XRepositoryAdapter` (método `@Query` no `XDataRepository`), devolvendo objetos de domínio.
3. Chame pela interface injetada no construtor; o CDI entrega a implementação.

Dados para **tela** (listas, detalhes, totais) não são da escrita. O endpoint de escrita devolve só o mínimo (`id`, `estado`).

## Leitura

- Injeta o `*QueryRepository` de `persistence/<agregado>/` direto, sem porta. Normaliza filtros e paginação e devolve os records projetados.
- **Não usa nada de `domain/`.**
