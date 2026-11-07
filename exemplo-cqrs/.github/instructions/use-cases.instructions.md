---
applyTo: "**/application/**"
---
# Use cases (command/application e query/application)

O use case é o Service Layer: orquestra e define a transação. Ele **não sabe qual canal o chamou** e não formata nada.

Escrita (`command/application`):
- Classe `*UseCase`, `@ApplicationScoped`, injeção por construtor, `@Transactional` no método público.
- Roteiro: buscar o agregado pelo repositório do domínio (`orElseThrow(() -> new NaoEncontradoException(...))`), chamar o método de negócio, salvar.
- A regra fica no domínio. Se o use case começar a ter `if` de negócio, mova para a entidade de domínio.
- Pode chamar `*Gateway` de `shared/gateway`. Faça a transição do domínio **antes** da chamada externa, para que uma transição inválida nem chegue ao sistema externo.
- Proibido importar `*Entity`, Panache, JPA, `command/persistence` ou qualquer coisa de `query`.

Precisa consultar algo para validar ou decidir a escrita?
1. Acrescente o método na interface de repositório do domínio (`command/domain/XRepository`), no vocabulário do negócio: `boolean existeProcessoAtivo(Long cotaId)`, `List<Cota> buscarPorCliente(Long clienteId)`.
2. Implemente no `XRepositoryAdapter` de `command/persistence` (Panache), devolvendo objetos de domínio.
3. Chame pela interface injetada no construtor; o CDI entrega a implementação.

Dados para **tela** (listas, detalhes, totais) não são da escrita: ficam no lado query. O endpoint de escrita devolve só o mínimo (`id`, `estado`).

Leitura (`query/application`):
- Injeta o repositório Jakarta Data de `query/persistence` direto (sem porta). Normaliza filtros e paginação; devolve os records projetados.
- Nunca importa nada de `command`.
