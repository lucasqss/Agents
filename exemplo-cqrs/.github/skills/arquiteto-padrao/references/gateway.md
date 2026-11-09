# Sistemas externos (`gateway/<sistema>/`)

Esta camada se organiza por **sistema externo**, e não por agregado: o mesmo sistema pode servir a vários agregados. Exemplo: `gateway/tesouraria/`.

Cada sistema tem seu pacote, com o padrão Gateway [PoEAA]:

- `XClient`: interface `@RegisterRestClient(configKey = "<sistema>")` com `@Timeout`, `@Retry` e `@CircuitBreaker` nos métodos. URL em `quarkus.rest-client.<sistema>.url=${<SISTEMA>_URL:...}`.
- DTOs no **vocabulário do sistema externo** (records), com os nomes de campo exatos da API. Eles nunca saem deste pacote (o ArchUnit falha).
- `XGateway` (`@ApplicationScoped`): API simples no nosso vocabulário (tipos simples ou records próprios), traduz request/response e converte qualquer falha em `SistemaExternoIndisponivelException` (vira 503). Essa exceção fica na raiz de `gateway/`.

Regras:

- O Gateway **não importa nada de `application/` nem de `domain/`.** Quem converte o retorno para o domínio é o caso de uso.
- Um sistema externo não depende de outro; quem combina dois sistemas é o caso de uso.
- Só casos de uso chamam gateways.
- `@RegisterRestClient`, `@Timeout`, `@Retry` e `@CircuitBreaker` só existem em `gateway/`.
- Nos testes, nunca chame o sistema real: crie um Service Stub (recurso JAX-RS fake, como `TesourariaFake`) e aponte `%test.quarkus.rest-client.<sistema>.url` para ele.
