---
applyTo: "**/command/domain/**"
---
# Domínio (command/domain)

Aqui mora a regra de negócio. É Java puro: o ArchUnit falha se aparecer import de JPA, Jakarta Data, CDI, JAX-RS, Quarkus, Hibernate, MicroProfile ou de qualquer pacote `shared`, `application`, `persistence`, `gateway` ou `canal`.

- Entidade de domínio protege as próprias invariantes: construtor ou fábrica estática (`novo`, `reconstituir`) que valida e lança `DominioException`. Sem setters públicos.
- Estados fechados usam o padrão State: `sealed interface` com `record`s (veja `estado/EstadoProcesso`). Transição inválida lança `TransicaoInvalidaException`.
- Sem Bean Validation (`@NotNull`, `@NotBlank`...) aqui: o formato é checado no `*Request` do canal; o domínio valida a regra de negócio no código e lança `DominioException`.
- Repositório é uma **interface** aqui (`XRepository`), no vocabulário do domínio. A implementação fica em `command/persistence`.
- Se uma regra do domínio precisar de um sistema externo, declare uma interface aqui; a implementação vai em `command/gateway/` delegando ao Gateway de `shared/gateway`. Nunca importe o Gateway no domínio.
- Todo comportamento novo ganha teste JUnit puro em `nucleo/src/test/java/**/command/domain/`.
- Regra que muda por canal (o que cada canal pode fazer) fica em `CanalDeAtendimento`, aqui no domínio, com teste em `CanalDeAtendimentoTest`.
