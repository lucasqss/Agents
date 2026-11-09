# Domínio (`domain/<agregado>/`)

Aqui mora a regra de negócio. É Java puro: o ArchUnit falha se aparecer import de JPA, Jakarta Data, CDI, JAX-RS, Quarkus, Hibernate, MicroProfile ou de `application/`, `persistence/`, `entrypoint/` ou `gateway/`.

- A entidade de domínio protege as próprias invariantes: construtor privado e fábricas estáticas (`novo`, `reconstituir`) que validam e lançam `DominioException`. Sem setters públicos. Acessores no estilo `nome()`, sem `get`.
- Estados fechados usam o padrão State [GoF]: `sealed interface` com `record`s ([exemplo](exemplos-codigo.md)). Transição inválida lança `TransicaoInvalidaException`. Estado novo entra em `permits` e em `todos()` da interface de estado.
- Variação por tipo usa o padrão Strategy [GoF]: uma implementação da interface de fluxo por tipo, escolhida pelo enum do tipo ([exemplo](exemplos-codigo.md)). Não crie subclasse do agregado por tipo.
- Toda classe de padrão traz no Javadoc uma linha curta com o padrão, o papel e a fonte. Ex.: `Padrão: State, estado concreto [GoF].`
- Sem Bean Validation (`@NotNull`, `@NotBlank`...) aqui: o formato é checado no entrypoint; o domínio valida a regra de negócio no código e lança `DominioException`.
- Repositório é uma **interface** aqui (`domain/cota/CotaRepository`), no vocabulário do negócio. A implementação fica em `persistence/cota/`.
- **Um agregado não importa outro.** A referência é só o id (`Long clienteId` em `domain/cota/Cota`). Verificar se o outro existe é papel do caso de uso. Objeto que pertence ao agregado vira `record` (Value Object) ou lista alterada só por métodos dele.
- Se uma regra precisar de um sistema externo, declare uma interface aqui. A implementação é um `gateway/<sistema>/XGatewayAdapter.java`, que delega ao `XGateway`. O domínio nunca importa o Gateway.
- Todo comportamento novo ganha teste JUnit puro, sem Quarkus.
