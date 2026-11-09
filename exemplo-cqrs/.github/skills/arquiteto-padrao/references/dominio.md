# Domínio

Aqui mora a regra de negócio. É Java puro: o ArchUnit falha se aparecer import de JPA, Jakarta Data, CDI, JAX-RS, Quarkus, Hibernate, MicroProfile ou de `application`, `persistence`, `entrypoint` ou `gateway`.

- A entidade de domínio protege as próprias invariantes: construtor privado e fábricas estáticas (`novo`, `reconstituir`) que validam e lançam `DominioException`. Sem setters públicos. Acessores no estilo `nome()`, sem `get`.
- Estados fechados usam o padrão State [GoF]: `sealed interface` com `record`s (veja `EstadoProcesso`). Transição inválida lança `TransicaoInvalidaException`. Estado novo entra em `permits` e em `EstadoProcesso.todos()`.
- Variação por tipo usa o padrão Strategy [GoF]: uma implementação de `FluxoProcesso` por tipo, escolhida pelo enum `TipoProcesso`. Não crie subclasse de `ProcessoPagamento` por tipo.
- Toda classe de padrão traz no Javadoc uma linha curta com o padrão, o papel e a fonte. Ex.: `Padrão: State, estado concreto [GoF].`
- Sem Bean Validation (`@NotNull`, `@NotBlank`...) aqui: o formato é checado no entrypoint; o domínio valida a regra de negócio no código e lança `DominioException`.
- Repositório é uma **interface** aqui (`XRepository`), no vocabulário do domínio. A implementação fica em `persistence`.
- Referência a outro agregado é só o id (`Long clienteId`). Objeto que pertence ao agregado vira `record` (Value Object) ou lista alterada só por métodos dele.
- Se uma regra do domínio precisar de um sistema externo, declare uma interface aqui. A implementação vai em `command/gateway/` (ou `<modulo>/gateway/`, sem CQRS), delegando ao Gateway do sistema. Nunca importe o Gateway no domínio.
- Todo comportamento novo ganha teste JUnit puro, sem Quarkus.
