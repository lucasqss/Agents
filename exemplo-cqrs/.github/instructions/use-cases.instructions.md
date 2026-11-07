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
- Proibido importar `*Entity`, Panache, JPA ou `command/persistence`.

Leitura (`query/application`):
- Injeta o repositório Jakarta Data de `query/persistence` direto (sem porta). Normaliza filtros e paginação; devolve os records projetados.
- Nunca importa nada de `command`.
